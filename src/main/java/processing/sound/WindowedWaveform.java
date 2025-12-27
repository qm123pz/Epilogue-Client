package processing.sound;

import com.jsyn.data.FloatSample;
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
public class WindowedWaveform extends Analyzer {

	public float[] data, dataRight;

	@Getter
	private FixedRateStereoWriterSpecial writer;
	private FloatSample buffer, bufferRight;
	private int lastAnalysisOffset, lastAnalysisOffsetRight;

	/**
	 * @param nsamples
	 *            number of waveform samples that you want to be able to read at once (a positive integer).
	 */
	public WindowedWaveform(int nsamples) {
		super();
		if (nsamples <= 0) {
			// TODO throw RuntimeException?
			processing.sound.Engine.printError("number of waveform frames needs to be greater than 0");
		} else {
			this.data = new float[nsamples];
			this.dataRight = new float[nsamples];

			this.writer = new FixedRateStereoWriterSpecial();
			this.buffer = new FloatSample(nsamples);
			this.bufferRight = new FloatSample(nsamples);
			// 改为一次性写入，不再循环
			this.writer.dataQueueLeft.queue(this.buffer);
			this.writer.dataQueueRight.queue(this.bufferRight);
		}
	}

	private UnitOutputPort connectedInput = null;

	public void removeInput() {
		processing.sound.Engine.getEngine().remove(this.writer);

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
		processing.sound.Engine.getEngine().add(this.writer);
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

	private boolean bufferFull = false; // 标记缓冲区是否已填满

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
			// 可选：保持警告或处理无输入情况
		}

		synchronized (this) {
			if (buffer.writeIndex < buffer.getBuffer().length - 1) {
				return value; // 尚未填满，返回空或当前值
			} else {

                // 直接读取整个缓冲区
                buffer.read(0, value, 0, buffer.getNumFrames());

                // 重新提交缓冲区以接收新数据
                writer.dataQueueLeft.queue(buffer);
				buffer.writeIndex = 0;
			}
		}
		return value;
	}

	public float[] analyzeRight(float[] value) {
		// 右声道同理
		synchronized (this) {
			if (bufferRight.writeIndex < bufferRight.getBuffer().length - 1) {
				return value;
			} else {
				bufferRight.read(0, value, 0, bufferRight.getNumFrames());
				writer.dataQueueRight.queue(bufferRight);
				bufferRight.writeIndex = 0;
			}

		}
		return value;
	}

	public void resize(int nsamples) {
		synchronized (this) {
			this.data = new float[nsamples];
			this.dataRight = new float[nsamples];
			this.buffer.allocate(nsamples, 1);
			this.bufferRight.allocate(nsamples, 1);

			// 重置队列并重新提交
			this.writer.dataQueueLeft = new UnitDataQueuePort("Data");
			this.writer.dataQueueRight = new UnitDataQueuePort("Data");
			this.writer.dataQueueLeft.queue(this.buffer);
			this.writer.dataQueueRight.queue(this.bufferRight);

			bufferFull = false; // 重置填充状态
		}
	}

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

}
