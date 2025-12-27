package processing.sound;

import com.jsyn.data.FloatSample;
import com.jsyn.ports.QueueDataEvent;
import com.jsyn.ports.UnitDataQueueCallback;
import com.jsyn.ports.UnitDataQueuePort;
import com.jsyn.ports.UnitOutputPort;
import com.jsyn.unitgen.FixedRateStereoWriterSpecial;
import lombok.Getter;


/**
 * This is a Waveform analyzer. It returns the waveform of an
 * audio stream the moment it is queried with the <b>getAnalyzedData()</b>
 * method.<br/>
 * Note that by default all processing.sound generators (including microphone capture from
 * <code>AudioIn</code>) have an amplitude of 1, which means that the values of
 * their waveform will be numbers in the range <code>[-0.5, 0.5]</code>.
 *
 * @author icalvin102
 *
 * @webref Analysis:Waveform
 * @webBrief Inspects the underlying soundwave of an audio signal.
 **/
public class Waveform extends Analyzer {

	public float[] data, dataRight;

	@Getter
	private FixedRateStereoWriterSpecial writer;
	private FloatSample buffer, bufferRight;
	private int lastAnalysisOffset, lastAnalysisOffsetRight;

	/**
	 * @param nsamples
	 *            number of waveform samples that you want to be able to read at once (a positive integer).
	 */
	public Waveform(float windowTime) {
		super();
		if (windowTime <= 0) {
			// TODO throw RuntimeException?
			Engine.printError("number of waveform frames needs to be greater than 0");
		} else {
			int nsamples = (int) (Engine.getEngine().getSampleRate() * windowTime);
			this.data = new float[nsamples];
			this.dataRight = new float[nsamples];

			this.writer = new FixedRateStereoWriterSpecial();
			this.buffer = new FloatSample(nsamples);
			this.bufferRight = new FloatSample(nsamples);
			// write any connected input into the output buffer ad infinitum
			this.writer.dataQueueLeft.queueLoop(this.buffer);
			this.writer.dataQueueRight.queueLoop(this.bufferRight);
//			this.connect(this.writer.dataQueueLeft, this.buffer, this.data);
//			this.connect(this.writer.dataQueueRight, this.bufferRight, this.dataRight);
		}
	}

	private void connect(UnitDataQueuePort port, FloatSample buffer, float[] data) {
		port.queueWithCallback(buffer, new UnitDataQueueCallback() {
			@Override
			public void started(QueueDataEvent event) {

			}

			@Override
			public void looped(QueueDataEvent event) {

			}

			@Override
			public void finished(QueueDataEvent event) {
				System.arraycopy(buffer.getBuffer(), 0, data, 0, data.length);
				port.queueWithCallback(buffer, this);
			}
		});

	}

	private UnitOutputPort connectedInput = null;

	public void removeInput() {
		Engine.getEngine().remove(this.writer);

		if (this.connectedInput != null) {
			this.writer.input.disconnect(0, connectedInput, 0);
			this.writer.input.disconnect(1, connectedInput, 1);
			this.connectedInput = null;
		} else {
			this.writer.input.disconnectAll();
		}

		this.input = null;
	}

	protected void setInput(UnitOutputPort input) {
		// superclass makes sure that input unit is actually playing, just connect it
		Engine.getEngine().add(this.writer);
		this.connectedInput = input;
		this.writer.input.connect(0, input, 0);
		this.writer.input.connect(1, input, 1);
		this.writer.start();
	}

	/**
	 * Gets the content of the current audiobuffer from the input source, writes it
	 * into this Waveform's `data` array, and returns it.
	 *
	 * @return the current audiobuffer of the input source. The array has as
	 *         many elements as this Waveform analyzer's number of samples
	 */
	public float[] analyze() {
		return this.analyze(this.data);
	}

	public float[] analyzeRight() {
		return this.analyzeRight(this.dataRight);
	}

	/**
	 * Gets the content of the current audiobuffer from the input source.
	 *
	 * @param value
	 *            an array with as many elements as this Waveform analyzer's number of
	 *            samples
	 * @return the current audiobuffer of the input source. The array has as
	 *         many elements as this Waveform analyzer's number of samples
	 * @webref Analysis:Waveform
	 * @webBrief Gets the content of the current audiobuffer from the input source.
	 **/
	public float[] analyze(float[] value) {
		if (this.input == null) {
//			Engine.printWarning("this Waveform has no processing.sound source connected to it, nothing to getAnalyzedData");
		}

//		return data;

		this.lastAnalysisOffset = (int) this.writer.dataQueueLeft.getFrameCount() % this.buffer.getNumFrames();
		// if initiating this read takes too long the first couple samples might actually
		// already be overwritten by the next loop, so fingers crossed...
		this.buffer.read(lastAnalysisOffset, value, 0, this.buffer.getNumFrames() - lastAnalysisOffset);
		this.buffer.read(0, value, this.buffer.getNumFrames() - lastAnalysisOffset, lastAnalysisOffset);
//		// the original implementation did a *2 on all values...?
		return value;
	}

	public float[] analyzeRight(float[] value) {
		if (this.input == null) {
//			Engine.printWarning("this Waveform has no processing.sound source connected to it, nothing to getAnalyzedData");
		}

//		return dataRight;

		this.lastAnalysisOffsetRight = (int) this.writer.dataQueueRight.getFrameCount() % this.bufferRight.getNumFrames();
		// if initiating this read takes too long the first couple samples might actually
		// already be overwritten by the next loop, so fingers crossed...
		this.bufferRight.read(lastAnalysisOffsetRight, value, 0, this.bufferRight.getNumFrames() - lastAnalysisOffsetRight);
		this.bufferRight.read(0, value, this.bufferRight.getNumFrames() - lastAnalysisOffsetRight, lastAnalysisOffsetRight);
		// the original implementation did a *2 on all values...?
		return value;
	}

/*
	public float[] analyzeCircular() {
		return this.analyzeCircular(this.data);
	}

	public float[] analyzeCircular(float[] value) {
		if (this.input == null) {
			Engine.printWarning("this Waveform has no processing.sound source connected to it, nothing to getAnalyzedData");
		}

		this.lastAnalysisOffset = (int) this.writer.dataQueue.getFrameCount() % this.buffer.getNumFrames();
		this.buffer.read(value);
		return value;
	}

	public int getLastAnalysisOffset() {
		return this.lastAnalysisOffset;
	}
*/

	// Below are just duplicated methods from superclasses which are required
	// for the online reference to build the corresponding pages.

	/**
	 * Define the audio input for the analyzer.
	 *
	 * @param input
	 *            the input processing.sound source. Can be an oscillator, noise generator,
	 *            SoundFile or AudioIn.
	 * @webref Analysis:Waveform
	 * @webBrief Define the audio input for the analyzer.
	 **/
	public void input(SoundObject input) {
		super.input(input);
	}

	public void resize(float windowTime) {
		int nsamples = (int) (Engine.getEngine().getSampleRate() * windowTime);

		this.data = new float[nsamples];
		this.dataRight = new float[nsamples];

//		this.writer = new FixedRateStereoWriterSpecial();

//		this.writer.dataQueueLeft.queueOff(this.buffer, true);
//		this.writer.dataQueueRight.queueOff(this.bufferRight, true);

		this.buffer.allocate(nsamples, 1);
		this.bufferRight.allocate(nsamples, 1);
//		this.buffer = new FloatSample(nsamples);
//		this.bufferRight = new FloatSample(nsamples);
		// write any connected input into the output buffer ad infinitum
		this.writer.dataQueueLeft = new UnitDataQueuePort("Data");
		this.writer.dataQueueRight = new UnitDataQueuePort("Data");
		this.writer.dataQueueLeft.queueLoop(this.buffer);
		this.writer.dataQueueRight.queueLoop(this.bufferRight);
//		this.connect(this.writer.dataQueueLeft, this.buffer, this.data);
//		this.connect(this.writer.dataQueueRight, this.bufferRight, this.dataRight);
	}
}
