package com.jsyn.util.soundfile.streamed.raf;

import com.jsyn.data.SampleMarker;
import com.jsyn.util.SampleLoader;
import com.jsyn.util.soundfile.IFFParser;
import com.jsyn.util.soundfile.WAVEFileParser;

import java.io.IOException;

/**
 * @author IzumiiKonata
 * Date: 2025/5/5 12:48
 */
public class RafWAVEFileParser extends WAVEFileParser {

    public RafFloatSample load(RafIFFParser parser) throws IOException {
        this.parser = parser;
        parser.parseAfterHead(this);
        return finish();
    }

    void parseFmtChunk(IFFParser parser, int ckSize) throws IOException {
        format = parser.readShortLittle();
        samplesPerFrame = parser.readShortLittle();
//        System.out.println("Samples Per Frame: " + samplesPerFrame);
        frameRate = parser.readIntLittle();
        parser.readIntLittle(); /* skip dwAvgBytesPerSec */
        blockAlign = parser.readShortLittle();
        bitsPerSample = parser.readShortLittle();
        bytesPerFrame = blockAlign;
        bytesPerSample = bytesPerFrame / samplesPerFrame;
        samplesPerBlock = (8 * blockAlign) / bitsPerSample;

        if (format == WAVE_FORMAT_EXTENSIBLE) {
            int extraSize = parser.readShortLittle();
            short validBitsPerSample = parser.readShortLittle();
            int channelMask = parser.readIntLittle();
            byte[] guid = new byte[16];
            parser.read(guid);
            if (matchBytes(guid, KSDATAFORMAT_SUBTYPE_IEEE_FLOAT)) {
                format = WAVE_FORMAT_IEEE_FLOAT;
            } else if (matchBytes(guid, KSDATAFORMAT_SUBTYPE_PCM)) {
                format = WAVE_FORMAT_PCM;
            }
        }
        if ((format != WAVE_FORMAT_PCM) && (format != WAVE_FORMAT_IEEE_FLOAT)) {
            throw new IOException(
                    "Only WAVE_FORMAT_PCM and WAVE_FORMAT_IEEE_FLOAT supported. format = " + format);
        }
        if ((bitsPerSample != 16) && (bitsPerSample != 24) && (bitsPerSample != 32)) {
            throw new IOException(
                    "Only 16 and 24 bit PCM or 32-bit float WAV files supported. width = "
                            + bitsPerSample);
        }
    }

    @Override
    public void handleChunk(IFFParser parser, int ckID, int ckSize) throws IOException {
//        System.out.println("!handleChunk, ckID: " + ckID + ", ckSize: " + ckSize);
        switch (ckID) {
            case FMT_ID:
//                System.out.println("ParseFmtChunk");
                parseFmtChunk(parser, ckSize);
                break;
            case DATA_ID:
//                System.out.println("ParseDataChunk");
                parseDataChunk(parser, ckSize);
                break;
            case CUE_ID:
                parseCueChunk(parser, ckSize);
                break;
            case FACT_ID:
                parseFactChunk(parser, ckSize);
                break;
            case SMPL_ID:
                parseSmplChunk(parser, ckSize);
                break;
            case LABL_ID:
                parseLablChunk(parser, ckSize);
                break;
            case LTXT_ID:
                parseLtxtChunk(parser, ckSize);
                break;
            default:
                break;
        }
    }

    private int convertByteToFrame(int byteOffset) throws IOException {
        if (blockAlign == 0) {
            throw new IOException("WAV file has bytesPerBlock = zero");
        }
        if (samplesPerFrame == 0) {
            throw new IOException("WAV file has samplesPerFrame = zero");
        }
        return (samplesPerBlock * byteOffset) / (samplesPerFrame * blockAlign);
    }

    private int calculateNumFrames(int numBytes) throws IOException {
        int nFrames;
        if (numFactSamples > 0) {
            // nFrames = numFactSamples / samplesPerFrame;
            nFrames = numFactSamples; // FIXME which is right
        } else {
            nFrames = convertByteToFrame(numBytes);
        }
        return nFrames;
    }

    StreamedByteDataReader reader;

    public static class StreamedByteDataReader {

        public final RafIFFParser parser;
        public final int bitsPerSample, numFrames, samplesPerFrame, format;

        private final long totalSize, startPos;

        public StreamedByteDataReader(RafIFFParser parser, int totalSize, int numFrames, int samplesPerFrame, int bitsPerSample, int format) throws IOException {
            this.parser = parser;
            this.totalSize = totalSize;
            this.bitsPerSample = bitsPerSample;
            this.numFrames = numFrames;
            this.samplesPerFrame = samplesPerFrame;
            this.format = format;
            this.startPos = parser.getPos();
        }

        byte[] byteData;
        float[] floatData;

        public final int floatArrLen = 4096;

        {
            int arrLen = this.getArrSize(floatArrLen);
            byteData = new byte[arrLen];
            floatData = new float[floatArrLen];
        }

        public int getSampleSizeInBytes() {
            int factor = 2;

            if (bitsPerSample == 24) {
                // 3b to 1f
                factor = 3;
            } else if (bitsPerSample == 32) {
                // 4b to 1f
                factor = 4;
            }

            return factor;
        }

        public int getArrSize(int outputSize) {
            return outputSize * getSampleSizeInBytes();
        }

        public int fillArrSize(int remaining) {

            int factor = this.getSampleSizeInBytes();

            if (remaining % factor != 0) {
                return remaining - remaining % factor;
            }

            return remaining;
        }

        public void seekToPosRelative(long pos) throws IOException {
            this.parser.seek(this.startPos + pos);
        }

        final float[] empty = new float[0];

        public float[] read(long pos) throws IOException {

            if (pos >= totalSize || this.startPos + pos >= parser.raf.length()) {
//                System.out.println("Returning empty because EOF");
                return empty;
            }

            long filePointer = parser.raf.getFilePointer();
            if (filePointer != this.startPos + pos) {
//                System.out.println("Seeking to " + (this.startPos + pos) + ", curPos: " + filePointer);
                this.seekToPosRelative(pos);
                filePointer = this.startPos + pos;
            }

            int arrSize = this.getArrSize(floatArrLen);

            if (totalSize - filePointer < arrSize) {
//                System.out.println("Adjusting array size cuz there's no enough data left");
//                System.out.println("totalSize: " + totalSize + ", filePointer: " + filePointer);
                int i = this.fillArrSize((int) (totalSize - filePointer));
                byteData = new byte[(int) (totalSize - filePointer)];
                floatData = new float[i / this.getSampleSizeInBytes()];
            } else if (byteData.length != arrSize) {
                byteData = new byte[arrSize];
                floatData = new float[floatArrLen];
            }

            int read = this.parser.read(byteData);

            if (read == -1) {
//                System.out.println("Read " + read + " bytes but expected " + arrSize);
//                System.out.println("Attempt to read at " + (this.startPos + pos) + ", parser pos: " + filePointer + ", total length: " + parser.raf.length());
                return empty;
            }

            int length = floatData.length * this.getSampleSizeInBytes();

            try {
                if (bitsPerSample == 16) {
                    SampleLoader.decodeLittleI16ToF32(byteData, 0, length, floatData, 0);
                } else if (bitsPerSample == 24) {
                    // 3b to 1f
                    SampleLoader.decodeLittleI24ToF32(byteData, 0, length, floatData, 0);
                } else if (bitsPerSample == 32) {
                    // 4b to 1f
                    if (format == WAVE_FORMAT_IEEE_FLOAT) {
                        SampleLoader.decodeLittleF32ToF32(byteData, 0, length, floatData, 0);
                    } else if (format == WAVE_FORMAT_PCM) {
                        SampleLoader.decodeLittleI32ToF32(byteData, 0, length, floatData, 0);
                    }
                }
            } catch (ArrayIndexOutOfBoundsException aioobe) {

                System.err.println("ArrayIndexOutOfBoundsException: byte[" + byteData.length + "], float[" + floatData.length + "], Sample Size Bytes: " + this.getSampleSizeInBytes() + ", Remaining: " + (totalSize - filePointer));

            }

            return floatData;
        }

    }

    public void parseDataChunk(IFFParser parser, int ckSize) throws IOException {
        dataPosition = parser.getOffset();

//        byteData = new byte[ckSize];
//        numRead = parser.read(byteData);
        numFrames = calculateNumFrames(ckSize);
        reader = new StreamedByteDataReader((RafIFFParser) parser, ckSize, numFrames, samplesPerFrame, bitsPerSample, format);
    }

    @Override
    public RafFloatSample finish() throws IOException {
        return makeSample(reader);
    }

    RafFloatSample makeSample(StreamedByteDataReader reader) throws IOException {
        RafFloatSample floatSample = new RafFloatSample(reader, numFrames, samplesPerFrame);

        floatSample.setChannelsPerFrame(samplesPerFrame);
        floatSample.setFrameRate(frameRate);
        floatSample.setPitch(originalPitch);

        if (sustainBegin >= 0) {
            floatSample.setSustainBegin(sustainBegin);
            floatSample.setSustainEnd(sustainEnd);
        }

        for (SampleMarker marker : cueMap.values()) {
            floatSample.addMarker(marker);
        }

        /* Set Sustain Loop by assuming first two markers are loop points. */
        if (floatSample.getMarkerCount() >= 2) {
            floatSample.setSustainBegin(floatSample.getMarker(0).position);
            floatSample.setSustainEnd(floatSample.getMarker(1).position);
        }
        return floatSample;
    }

}
