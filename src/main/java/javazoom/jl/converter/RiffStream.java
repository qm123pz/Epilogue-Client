/*
 * 11/19/04 1.0 moved to LGPL.
 * 02/23/99 JavaConversion by E.B
 * Don Cross, April 1993.
 * RIFF file format classes.
 * See Chapter 8 of "Multimedia Programmer's Reference" in
 * the Microsoft Windows SDK.
 *
 *-----------------------------------------------------------------------
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *----------------------------------------------------------------------
 */

package javazoom.jl.converter;

import lombok.Getter;

import java.io.IOException;


/**
 * Class to manage RIFF files
 */
public class RiffStream {

    static class RiffChunkHeader {
        /** Four-character chunk ID */
        public int ckID = 0;
        /** Length of data in chunk */
        public int ckSize = 0;
    }

    // DDCRET

    /** The operation succeeded */
    public static final int DDC_SUCCESS = 0;
    /** The operation failed for unspecified reasons */
    public static final int DDC_FAILURE = 1;
    /** Operation failed due to running out of memory */
    public static final int DDC_OUT_OF_MEMORY = 2;
    /** Operation encountered file I/O error */
    public static final int DDC_FILE_ERROR = 3;
    /** Operation was called with invalid parameters */
    public static final int DDC_INVALID_CALL = 4;
    /** Operation was aborted by the user */
    public static final int DDC_USER_ABORT = 5;
    /** File format does not match */
    public static final int DDC_INVALID_FILE = 6;

    // RiffFileMode

    /** undefined type (can use to mean "N/A" or "not open") */
    public static final int RFM_UNKNOWN = 0;
    /** open for write */
    public static final int RFM_WRITE = 1;
    /** open for read */
    public static final int RFM_READ = 2;

    /** header for whole file */
    private RiffChunkHeader riffHeader;
    /** current file I/O mode */
    protected int fmode;
    /** I/O stream to use */
    @Getter
    protected SeekableByteArrayOutputStream stream;

    /**
     * Dummy Constructor
     */
    public RiffStream() {
        stream = null;
        fmode = RFM_UNKNOWN;
        riffHeader = new RiffChunkHeader();

        riffHeader.ckID = fourCC("RIFF");
        riffHeader.ckSize = 0;
    }

    /**
     * Return File Mode.
     */
    public int CurrentFileMode() {
        return fmode;
    }

    /**
     * Open a RIFF file.
     */
    public int Open() {
        int retcode = DDC_SUCCESS;

        if (fmode != RFM_UNKNOWN) {
            retcode = close();
        }

        if (retcode == DDC_SUCCESS) {
            this.stream = new SeekableByteArrayOutputStream();

            // Write the RIFF header...
            // We will have to come back later and patch it!
            byte[] br = new byte[8];
            br[0] = (byte) ((riffHeader.ckID >>> 24) & 0x000000FF);
            br[1] = (byte) ((riffHeader.ckID >>> 16) & 0x000000FF);
            br[2] = (byte) ((riffHeader.ckID >>> 8) & 0x000000FF);
            br[3] = (byte) (riffHeader.ckID & 0x000000FF);

            byte br4 = (byte) ((riffHeader.ckSize >>> 24) & 0x000000FF);
            byte br5 = (byte) ((riffHeader.ckSize >>> 16) & 0x000000FF);
            byte br6 = (byte) ((riffHeader.ckSize >>> 8) & 0x000000FF);
            byte br7 = (byte) (riffHeader.ckSize & 0x000000FF);

            br[4] = br7;
            br[5] = br6;
            br[6] = br5;
            br[7] = br4;

            stream.write(br, 0, 8);
            fmode = RFM_WRITE;
        }
        return retcode;
    }

    /**
     * Write numBytes data.
     */
    public int write(byte[] data, int numBytes) {
        if (fmode != RFM_WRITE) {
            return DDC_INVALID_CALL;
        }
        stream.write(data, 0, numBytes);
        riffHeader.ckSize += numBytes;
        return DDC_SUCCESS;
    }


    /**
     * Write numBytes data.
     */
    public int write(short[] data, int numBytes) {
        byte[] theData = new byte[numBytes];
        int yc = 0;
        for (int y = 0; y < numBytes; y = y + 2) {
            theData[y] = (byte) (data[yc] & 0x00FF);
            theData[y + 1] = (byte) ((data[yc++] >>> 8) & 0x00FF);
        }
        if (fmode != RFM_WRITE) {
            return DDC_INVALID_CALL;
        }
        stream.write(theData, 0, numBytes);
        riffHeader.ckSize += numBytes;
        return DDC_SUCCESS;
    }

    /**
     * Write numBytes data.
     */
    public int write(RiffChunkHeader triffHeader, int numBytes) {
        byte[] br = new byte[8];
        br[0] = (byte) ((triffHeader.ckID >>> 24) & 0x000000FF);
        br[1] = (byte) ((triffHeader.ckID >>> 16) & 0x000000FF);
        br[2] = (byte) ((triffHeader.ckID >>> 8) & 0x000000FF);
        br[3] = (byte) (triffHeader.ckID & 0x000000FF);

        byte br4 = (byte) ((triffHeader.ckSize >>> 24) & 0x000000FF);
        byte br5 = (byte) ((triffHeader.ckSize >>> 16) & 0x000000FF);
        byte br6 = (byte) ((triffHeader.ckSize >>> 8) & 0x000000FF);
        byte br7 = (byte) (triffHeader.ckSize & 0x000000FF);

        br[4] = br7;
        br[5] = br6;
        br[6] = br5;
        br[7] = br4;

        if (fmode != RFM_WRITE) {
            return DDC_INVALID_CALL;
        }
        stream.write(br, 0, numBytes);
        riffHeader.ckSize += numBytes;
        return DDC_SUCCESS;
    }

    public final void writeShort(int v) throws IOException {
        stream.write((v >>> 8) & 0xFF);
        stream.write((v >>> 0) & 0xFF);
        //written += 2;
    }

    /**
     * Write numBytes data.
     */
    public int write(short data, int numBytes) {
        short theData = (short) (((data >>> 8) & 0x00FF) | ((data << 8) & 0xFF00));
        if (fmode != RFM_WRITE) {
            return DDC_INVALID_CALL;
        }
        try {
            writeShort(theData);
            fmode = RFM_WRITE;
        } catch (IOException ioe) {
            return DDC_FILE_ERROR;
        }
        riffHeader.ckSize += numBytes;
        return DDC_SUCCESS;
    }

    public final void writeInt(int v) throws IOException {
        stream.write((v >>> 24) & 0xFF);
        stream.write((v >>> 16) & 0xFF);
        stream.write((v >>>  8) & 0xFF);
        stream.write((v >>>  0) & 0xFF);
        //written += 4;
    }

    /**
     * Write numBytes data.
     */
    public int write(int data, int numBytes) {
        short theDataL = (short) ((data >>> 16) & 0x0000FFFF);
        short theDataR = (short) (data & 0x0000FFFF);
        short theDataLI = (short) (((theDataL >>> 8) & 0x00FF) | ((theDataL << 8) & 0xFF00));
        short theDataRI = (short) (((theDataR >>> 8) & 0x00FF) | ((theDataR << 8) & 0xFF00));
        int theData = ((theDataRI << 16) & 0xFFFF0000) | (theDataLI & 0x0000FFFF);
        if (fmode != RFM_WRITE) {
            return DDC_INVALID_CALL;
        }
        try {
            writeInt(theData);
            fmode = RFM_WRITE;
        } catch (IOException ioe) {
            return DDC_FILE_ERROR;
        }
        riffHeader.ckSize += numBytes;
        return DDC_SUCCESS;
    }

    /**
     * Read numBytes data.
     */
    public int read(byte[] data, int numBytes) {
        return DDC_FILE_ERROR;
    }

    /**
     * Expect numBytes data.
     */
    public int expect(String data, int numBytes) {
        return DDC_FILE_ERROR;
    }

    /**
     * Close Riff File.
     * Length is written too.
     */
    public int close() {
        int retcode = DDC_SUCCESS;

        stream.seek(0);
        try {
            byte[] br = new byte[8];
            br[0] = (byte) ((riffHeader.ckID >>> 24) & 0x000000FF);
            br[1] = (byte) ((riffHeader.ckID >>> 16) & 0x000000FF);
            br[2] = (byte) ((riffHeader.ckID >>> 8) & 0x000000FF);
            br[3] = (byte) (riffHeader.ckID & 0x000000FF);

            br[7] = (byte) ((riffHeader.ckSize >>> 24) & 0x000000FF);
            br[6] = (byte) ((riffHeader.ckSize >>> 16) & 0x000000FF);
            br[5] = (byte) ((riffHeader.ckSize >>> 8) & 0x000000FF);
            br[4] = (byte) (riffHeader.ckSize & 0x000000FF);
            stream.write(br, 0, 8);
            stream.close();
        } catch (IOException ioe) {
            retcode = DDC_FILE_ERROR;
        }
        fmode = RFM_UNKNOWN;
        return retcode;
    }

    /**
     * Write data to specified offset.
     */
    public int backpatch(long fileOffset, RiffChunkHeader data, int numBytes) {
        if (stream == null) {
            return DDC_INVALID_CALL;
        }
        stream.seek(fileOffset);
        return write(data, numBytes);
    }

    public int backpatch(long fileOffset, byte[] data, int numBytes) {
        if (stream == null) {
            return DDC_INVALID_CALL;
        }
        stream.seek(fileOffset);
        return write(data, numBytes);
    }

    /**
     * Seek in the File.
     */
    protected int seek(long offset) {
        int rc;
        stream.seek(offset);
        rc = DDC_SUCCESS;
        return rc;
    }

    /**
     * Error Messages.
     */
    private String toDDCRETString(int retcode) {
        switch (retcode) {
            case DDC_SUCCESS:
                return "DDC_SUCCESS";
            case DDC_FAILURE:
                return "DDC_FAILURE";
            case DDC_OUT_OF_MEMORY:
                return "DDC_OUT_OF_MEMORY";
            case DDC_FILE_ERROR:
                return "DDC_FILE_ERROR";
            case DDC_INVALID_CALL:
                return "DDC_INVALID_CALL";
            case DDC_USER_ABORT:
                return "DDC_USER_ABORT";
            case DDC_INVALID_FILE:
                return "DDC_INVALID_FILE";
            default:
                return "Unknown Error";
        }
    }

    /**
     * Fill the header.
     */
    public static int fourCC(String chunkName) {
        byte[] p = chunkName.getBytes();
        int ret = (((p[0] << 24) & 0xFF000000) | ((p[1] << 16) & 0x00FF0000) | ((p[2] << 8) & 0x0000FF00) | (p[3] & 0x000000FF));
        return ret;
    }

    public long currentFilePosition() {
        return stream.getWritePointer();
    }
}
