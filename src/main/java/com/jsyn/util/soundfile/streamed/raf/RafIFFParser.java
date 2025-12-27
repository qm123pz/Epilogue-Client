package com.jsyn.util.soundfile.streamed.raf;

import com.jsyn.util.soundfile.ChunkHandler;
import com.jsyn.util.soundfile.IFFParser;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author IzumiiKonata
 * Date: 2025/5/5 12:50
 */
public class RafIFFParser extends IFFParser {

    public final RandomAccessFile raf;

    RafIFFParser(RandomAccessFile raf) {
        super(null);
        this.raf = raf;
        numBytesRead = 0;
    }

    /**
     * Size of file based on outermost chunk size plus 8. Can be used to report progress when
     * loading samples.
     *
     * @return Number of bytes in outer chunk plus header.
     */
    @Override
    public long getFileSize() {
        return totalSize;
    }

    /**
     * Since IFF files use chunks with explicit size, it is important to keep track of how many
     * bytes have been read from the file. Can be used to report progress when loading samples.
     *
     * @return Number of bytes read from stream, or skipped.
     */
    @Override
    @SneakyThrows
    public long getOffset() {
        return raf.getFilePointer();
    }

    /** @return Next byte from stream. Increment offset by 1. */
    @Override
    public int read() throws IOException {
        numBytesRead++;
        return raf.read();
    }

    /** @return Next byte array from stream. Increment offset by len. */
    @Override
    public int read(byte[] bar, int off, int len) throws IOException {
        // Reading from a URL can return before all the bytes are available.
        // So we keep reading until we get the whole thing.
        int cursor = off;
        int numLeft = len;
        // keep reading data until we get it all
        while (numLeft > 0) {
            int numRead = raf.read(bar, cursor, numLeft);
            if (numRead < 0)
                return numRead;
            cursor += numRead;
            numBytesRead += numRead;
            numLeft -= numRead;
            // LOGGER.debug("read " + numRead + ", cursor = " + cursor +
            // ", len = " + len);
        }
        return cursor - off;
    }

    /** @return Skip forward in stream and add numBytes to offset. */
    @Override
    public long skip(long numBytes) throws IOException {
        numBytesRead += numBytes;
        raf.seek(raf.getFilePointer() + numBytes);
        return raf.getFilePointer();
    }

    public void seek(long pos) throws IOException {
        raf.seek(pos);
        numBytesRead = raf.getFilePointer();
    }

    public long getPos() throws IOException {
        return raf.getFilePointer();
    }

    /**
     * Parse the stream after reading the first ID and pass the forms and chunks to the ChunkHandler
     */
    @Override
    public void parseAfterHead(ChunkHandler handler) throws IOException {
        int numBytes = readChunkSize();
        totalSize = numBytes + 8;
        parseChunk(handler, fileId, numBytes);
    }

    /**
     * Parse the FORM and pass the chunks to the ChunkHandler The cursor should be positioned right
     * after the type field.
     */
    @Override
    public void parseForm(ChunkHandler handler, int ID, int numBytes, int type) throws IOException {
        while (numBytes > 8) {
            int ckid = readIntBig();
            int size = readChunkSize();
            numBytes -= 8;
            if (size < 0) {
                throw new IOException("Bad IFF chunk Size: " + IDToString(ckid) + " = 0x"
                        + Integer.toHexString(ckid) + ", Size = " + size);
            }
            parseChunk(handler, ckid, size);
            if ((size & 1) == 1)
                size++; // even-up
            numBytes -= size;
        }

        if (numBytes > 0) {
            skip(numBytes);
        }
    }

    /*
     * Parse one chunk from IFF file. After calling handler, make sure stream is positioned at end
     * of chunk.
     */
    @Override
    public void parseChunk(ChunkHandler handler, int ckid, int numBytes) throws IOException {
        long startOffset, endOffset;
        int numRead;
        startOffset = getOffset();
        if (isForm(ckid)) {
            int type = readIntBig();
            handler.handleForm(this, ckid, numBytes - 4, type);
            endOffset = getOffset();
            numRead = (int) (endOffset - startOffset);
            if (numRead < numBytes)
                parseForm(handler, ckid, (numBytes - numRead), type);
        } else {
            handler.handleChunk(this, ckid, numBytes);
        }
        endOffset = getOffset();
        numRead = (int) (endOffset - startOffset);
        if ((numBytes & 1) == 1)
            numBytes++; // even-up
        if (numRead < numBytes)
            skip(numBytes - numRead);
    }

}
