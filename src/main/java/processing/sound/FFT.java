package processing.sound;

import com.jsyn.ports.UnitOutputPort;
import com.softsynth.math.FourierMath;
import lombok.Getter;


/**
 * This is a Fast Fourier Transform (FFT) analyzer. It calculates the normalized
 * power spectrum of an audio stream the moment it is queried with the getAnalyzedData()
 * method.
 * 
 * @webref Analysis:FFT
 * @webBrief This is a Fast Fourier Transform (FFT) analyzer.
 **/
public class FFT extends Analyzer {

	@Getter
	private JSynFFT fft;

	/**
	 * @param bufferSize
	 *            number of frequency bufferSize for the FFT. This parameter needs to
	 *            be a power of 2 (e.g. 16, 32, 64, 128, ...). The default is 512.
	 */
	public FFT(int bufferSize, JSynFFT.FFTCalcCallback callback) {
		super();
		if (FFT.checkNumBands(bufferSize)) {
			// if we want to be able to detect something for the highest band, the FFT 
			// buffer size needs to be twice the number of frequency bufferSize
			this.fft = new JSynFFT(bufferSize, callback);
		}
	}

	private static boolean checkNumBands(int bands) {
		// just print an error message instead of interrupting the whole sketch 
		// executing with a RuntimeException
		if (bands < 0 || Integer.bitCount(bands) != 1) {
			Engine.printError("number of FFT bands needs to be a power of 2");
			return false;
		} else if (bands > 16384) {
			Engine.printError("the maximum number of FFT bands is 16384");
			return false;
		}
		return true;
	}

	private UnitOutputPort connectedInput = null;

	public void removeInput() {
		Engine.getEngine().remove(this.fft);

		if (this.connectedInput != null) {
			this.fft.input.disconnect(0, connectedInput, 0);
			this.fft.input.disconnect(1, connectedInput, 1);
			this.connectedInput = null;
		} else {
			this.fft.input.disconnectAll();
		}

		this.input = null;
	}

	protected void setInput(UnitOutputPort input) {
		// superclass makes sure that input unit is actually playing, just connect it
		Engine.getEngine().add(this.fft);
		this.connectedInput = input;
		this.fft.input.connect(0, input, 0);
		this.fft.input.connect(1, input, 1);
		this.fft.start();
	}

	/**
	 * Calculates the current frequency spectrum of the input signal.
	 * Returns an array with as many elements as this FFT analyzer's number of 
	 * frequency bands. The frequency associated with each band of the spectrum is
	 * <code>frequency = binIndex * sampleRate / (2*numBands)</code>.<br>
	 *
	 * The values of the resulting array show the amplitudes of pure tone 
	 * components contained in the signal. If the signal is a sine with an 
	 * amplitude of 1, the spectrum will have an absolute value of 1 (0 dB) at the 
	 * frequency of the sine. For complex real-world signals the spectrum values 
	 * will be much lower and usually don't exceed 0.05.
	 * @param target
	 *            if provided, writes the frequency spectrum into the given array.
	 *            The array needs to have as many elements as this FFT analyzer's 
	 *            number of frequency bands.
	 * @webref Analysis:FFT
	 * @webBrief Calculates the current frequency spectrum of the audio input 
	 * signal.
	 **/
	public float[] getAnalyzedData() {
		if (this.input == null) {
			Engine.printWarning("this FFT has no processing.sound source connected to it, nothing to getAnalyzedData");
		}
		return this.fft.calculateMagnitudes();
	}

	/**
	 * Calculates the frequency spectrum of a given audio sample and returns an 
	 * array of magnitudes, one for each frequency band. The frequency associated 
	 * with each band of the spectrum is <code>frequency = binIndex * sampleRate / 
	 * (2*numBands)</code>.<br>
	 * This version is intended to be used in non-real time processing, particularly when you are
	 * creating an animation in non-real time and want to get the FFT for a particular chunk of an audio sample.
	 *
	 * For stereo samples, you can call this function once for each channel, so you can display the left and right
	 * fft values separately.<br>
	 *
	 * The values of the resulting array show the amplitudes of pure tone 
	 * components contained in the signal. If the signal is a sine with an 
	 * amplitude of 1, the spectrum will have an absolute value of 1 (0 dB) at the 
	 * frequency of the sine. For complex real-world signals the spectrum values 
	 * will be much lower and usually don't exceed 0.05.
	 * @param sample
	 *            an array of numbers that describe the waveform to be analyzed
	 * @param numBands
	 *            the number of fft bands requested. Must be a power of 2 (one of 2, 4, 8, 16 etc.)
	 * @param target array that the computed spectrum will be written to. The FFT 
	 * will compute as many frequency bands as the length of this array, which 
	 * must be a power of 2 (2, 4, 8, 16 etc.)
	 * @return The frequency spectrum of the given audio sample. The array has as
	 *         many elements as this FFT analyzer's number of frequency bands.
	 * @webref Analysis:FFT
	 * @webBrief Calculates the frequency spectrum of a given audio sample.
	 **/
	public static float[] analyzeSample(float[] sample, float[] target) {
		FFT.calculateMagnitudesFromSample(sample, target);
		return target;
	}

	public static float[] analyzeSample(float[] sample, int numBands) {
		return FFT.analyzeSample(sample, new float[numBands]);
	}

	// the meat of the matter
	protected static void calculateMagnitudesFromSample(float[] sample, float[] imaginary, float[] target) {
		if (FFT.checkNumBands(target.length)) {
			FourierMath.transform(1, target.length, sample, imaginary);
			FourierMath.calculateMagnitudes(sample, imaginary, target);
			// there is an argument for multiplying the normalized spectrum amplitude 
			// values by two, see e.g.:
			// https://pyfar.readthedocs.io/en/stable/concepts/pyfar.fft.html#fft-normalizations
			// https://de.mathworks.com/matlabcentral/answers/162846-amplitude-of-signal-after-fft-operation#answer_159088
//			for (int i = 0; i < target.length; i++) {
//				target[i] *= 2;
//			}
		}
	}

	protected static void calculateMagnitudesFromSample(float[] sample, float[] target) {
		FFT.calculateMagnitudesFromSample(sample, new float[sample.length], target);
	}

}
