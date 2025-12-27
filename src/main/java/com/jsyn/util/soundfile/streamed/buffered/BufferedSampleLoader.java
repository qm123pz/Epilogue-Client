package com.jsyn.util.soundfile.streamed.buffered;

import com.jsyn.data.FloatSample;
import com.jsyn.util.soundfile.CustomSampleLoader;
import com.jsyn.util.soundfile.WAVEFileParser;
import lombok.SneakyThrows;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.sound.spi.Flac2PcmAudioInputStream;
import org.kc7bfi.jflac.sound.spi.FlacAudioFileReader;
import org.kc7bfi.jflac.sound.spi.FlacEncoding;

import javax.sound.sampled.AudioFormat;
import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * @author IzumiiKonata
 * Date: 2025/5/5 12:47
 */
public class BufferedSampleLoader extends CustomSampleLoader {

    public FloatSample loadFloatSample(BufferedInputStream is) throws IOException {
        BufferedIFFParser parser = new BufferedIFFParser(is);
        parser.readHead();
        if (parser.isRIFF()) {
            BufferedWAVEFileParser fileParser = new BufferedWAVEFileParser();
            return fileParser.load(parser);
        } else if (parser.isIFF()) {
            throw new UnsupportedOperationException("AIFF is not supported");
        } else {
            throw new IOException("Unsupported audio file type.");
        }
    }

    @SneakyThrows
    public FloatSample loadFromFlacStream(BufferedInputStream is) throws IOException {
        FlacAudioFileReader fafr = new FlacAudioFileReader();
        is.mark(0);

        StreamInfo streamInfo = fafr.getStreamInfo(is);

        int sampleRate = streamInfo.getSampleRate();
        long totalSamples = streamInfo.getTotalSamples();

        is.reset();

        BufferedInputStream is1 = new BufferedInputStream(new Flac2PcmAudioInputStream(is,
                new AudioFormat(
                        FlacEncoding.FLAC, sampleRate, (int) (streamInfo.getBitsPerSample() * totalSamples), streamInfo.getChannels(), streamInfo.getBitsPerSample(), sampleRate, false
                ),
                totalSamples),
                (int) (streamInfo.getBitsPerSample() * totalSamples / 4)
        );

        is1.mark(0);

//        System.out.println("(int) streamInfo.getTotalSamples() = " + (int) totalSamples);
//        System.out.println("streamInfo.getChannels() = " + streamInfo.getChannels());
//        System.out.println("streamInfo.getBitsPerSample() = " + streamInfo.getBitsPerSample());
//        System.out.println("streamInfo.getSampleRate() = " + sampleRate);

        BufferedIFFParser parser = new BufferedIFFParser(is1) {

        };
        parser.totalBytes = streamInfo.getBitsPerSample() * totalSamples * 8;
        BufferedWAVEFileParser.StreamedByteDataReader reader = new BufferedWAVEFileParser.StreamedByteDataReader(parser, streamInfo.getBitsPerSample() * totalSamples * 8, (int) totalSamples * streamInfo.getChannels(), streamInfo.getChannels(), streamInfo.getBitsPerSample(), WAVEFileParser.WAVE_FORMAT_PCM);

        BufferedFloatSample floatSample = new BufferedFloatSample(reader, (int) totalSamples, streamInfo.getChannels());

        floatSample.setChannelsPerFrame(streamInfo.getChannels());
        floatSample.setFrameRate(sampleRate);
        floatSample.setPitch(60.0);

        return floatSample;
    }

}
