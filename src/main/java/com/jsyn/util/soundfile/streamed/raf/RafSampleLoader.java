package com.jsyn.util.soundfile.streamed.raf;

import com.jsyn.data.FloatSample;
import com.jsyn.util.soundfile.CustomSampleLoader;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author IzumiiKonata
 * Date: 2025/5/5 12:47
 */
public class RafSampleLoader extends CustomSampleLoader {

    public FloatSample loadFloatSample(RandomAccessFile raf) throws IOException {
        RafIFFParser parser = new RafIFFParser(raf);
        parser.readHead();
        if (parser.isRIFF()) {
            RafWAVEFileParser fileParser = new RafWAVEFileParser();
            return fileParser.load(parser);
        } else if (parser.isIFF()) {
            throw new UnsupportedOperationException("AIFF is not supported");
        } else {
            throw new IOException("Unsupported audio file type.");
        }
    }

}
