package cn.edu.tsinghua.tsfile.read.reader.chunk;

import cn.edu.tsinghua.tsfile.common.conf.TSFileDescriptor;
import cn.edu.tsinghua.tsfile.compress.UnCompressor;
import cn.edu.tsinghua.tsfile.encoding.decoder.Decoder;
import cn.edu.tsinghua.tsfile.file.header.ChunkHeader;
import cn.edu.tsinghua.tsfile.file.header.PageHeader;
import cn.edu.tsinghua.tsfile.file.metadata.enums.TSDataType;
import cn.edu.tsinghua.tsfile.file.metadata.enums.TSEncoding;
import cn.edu.tsinghua.tsfile.read.filter.basic.Filter;
import cn.edu.tsinghua.tsfile.read.common.Chunk;
import cn.edu.tsinghua.tsfile.read.common.BatchData;
import cn.edu.tsinghua.tsfile.read.reader.page.PageReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;


public abstract class ChunkReader {

    ChunkHeader chunkHeader;
    private ByteBuffer chunkDataBuffer;

    private UnCompressor unCompressor;
    private Decoder valueDecoder;
    private Decoder timeDecoder = Decoder.getDecoderByType(TSEncoding.valueOf(TSFileDescriptor.getInstance().getConfig().timeSeriesEncoder)
            , TSDataType.INT64);

    private Filter filter;

    private BatchData data;

    private long maxTombstoneTime;


    public ChunkReader(Chunk chunk) {
        this(chunk, null);
    }

    public ChunkReader(Chunk chunk, Filter filter) {
        this.filter = filter;
        this.chunkDataBuffer = chunk.getData();
        chunkHeader = chunk.getHeader();
        this.unCompressor = UnCompressor.getUnCompressor(chunkHeader.getCompressionType());
        valueDecoder = Decoder.getDecoderByType(chunkHeader.getEncodingType(), chunkHeader.getDataType());
        data = new BatchData(chunkHeader.getDataType());
    }


    public boolean hasNextBatch() {
        return chunkDataBuffer.remaining() > 0;
    }

    public BatchData nextBatch() throws IOException {

        // construct next satisfied page header
        while (chunkDataBuffer.remaining() > 0) {
            // deserialize a PageHeader from chunkDataBuffer
            PageHeader pageHeader = PageHeader.deserializeFrom(chunkDataBuffer, chunkHeader.getDataType());

            // if the current page satisfies
            if (pageSatisfied(pageHeader)) {
                PageReader pageReader = constructPageReaderForNextPage(pageHeader.getCompressedSize());
                if (pageReader.hasNextBatch()) {
                    data = pageReader.nextBatch();
                    return data;
                }
            } else {
                skipBytesInStreamByLength(pageHeader.getCompressedSize());
            }
        }

        return data;
    }

    public BatchData currentBatch() {
        return data;
    }

    private void skipBytesInStreamByLength(long length) {
        chunkDataBuffer.position(chunkDataBuffer.position() + (int) length);
    }

    public abstract boolean pageSatisfied(PageHeader pageHeader);


    private PageReader constructPageReaderForNextPage(int compressedPageBodyLength)
            throws IOException {
        byte[] compressedPageBody = new byte[compressedPageBodyLength];

        // already in memory
        if (compressedPageBodyLength > chunkDataBuffer.remaining())
            throw new IOException("unexpected byte read length when read compressedPageBody. Expected:"
                    + Arrays.toString(compressedPageBody) + ". Actual:" + chunkDataBuffer.remaining());

        chunkDataBuffer.get(compressedPageBody, 0, compressedPageBodyLength);
        valueDecoder.reset();
        return new PageReader(ByteBuffer.wrap(unCompressor.uncompress(compressedPageBody)),
                chunkHeader.getDataType(), valueDecoder, timeDecoder, filter);
    }

    public void close() {
    }

    public void setMaxTombstoneTime(long maxTombStoneTime) {
        this.maxTombstoneTime = maxTombStoneTime;
    }

    public long getMaxTombstoneTime() {
        return this.maxTombstoneTime;
    }


}