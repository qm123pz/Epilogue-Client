package processing.sound;

import com.jsyn.data.FloatSample;
import com.jsyn.ports.QueueDataEvent;
import com.jsyn.ports.UnitDataQueueCallback;
import com.jsyn.unitgen.FixedRateStereoWriterToMono;
import com.tagtraum.jipes.math.FFTFactory;

import java.util.Arrays;

/**
 * This class copies all input to an audio buffer of the given size and performs
 * an FFT on it when required.
 */
public class JSynFFT extends FixedRateStereoWriterToMono {

	private FloatSample buffer;
	private float[] real, imaginary;
	private float[] realOut, imagOut;
	private float[] fftOut;
	FFTFactory.JavaFFT fft;

	public final int bufferSize;

	public static final int FFT_SIZE = 4096;

	public interface FFTCalcCallback {
		void onFFT(float[] fftData);
	}

	final FFTCalcCallback callback;

	protected JSynFFT(int bufferSize, FFTCalcCallback callback) {
		super();
		this.callback = callback;
		this.bufferSize = bufferSize;
		this.buffer = new FloatSample(bufferSize);
		this.real = new float[FFT_SIZE];
		this.imaginary = new float[FFT_SIZE];
		this.realOut = new float[FFT_SIZE];
		this.imagOut = new float[FFT_SIZE];
		this.fftOut = new float[FFT_SIZE];

		this.fft = new FFTFactory.JavaFFT(FFT_SIZE);

		// write any connected input into the output buffer ad infinitum

		this.dataQueue.queueWithCallback(this.buffer, new UnitDataQueueCallback() {
			@Override
			public void started(QueueDataEvent event) {

			}

			@Override
			public void looped(QueueDataEvent event) {

			}

			@Override
			public void finished(QueueDataEvent event) {

				// 左移现有数据，为新数据腾出空间
				System.arraycopy(real, bufferSize, real, 0, FFT_SIZE - bufferSize);

				// 从音频缓冲区读取新数据并添加到 real 数组尾部
				float[] bufferData = buffer.getBuffer();
				System.arraycopy(bufferData, 0, real, FFT_SIZE - bufferSize, bufferSize);

				// 清空虚部
				Arrays.fill(imaginary, 0.0f);

				// 执行 FFT
				fft.transform(false, real, imaginary, realOut, imagOut);

				// 计算频率点幅值
				for (int i = 0; i < FFT_SIZE; i++) {
					fftOut[i] = (float) hypot(realOut[i] / FFT_SIZE, imagOut[i] / FFT_SIZE);
				}

				callback.onFFT(fftOut);

				// 重新排队处理下一个缓冲区
				dataQueue.queueWithCallback(buffer, this);
			}
		});
	}

    private final int MIN_DOUBLE_EXPONENT = -1074;
    private final int MAX_DOUBLE_EXPONENT = 1023;
    final int SQRT_LO_BITS = 12;
    final int SQRT_LO_TAB_SIZE = (1<<SQRT_LO_BITS);
    final double[] sqrtXSqrtHiTab = new double[MAX_DOUBLE_EXPONENT-MIN_DOUBLE_EXPONENT+1];
    final double[] sqrtXSqrtLoTab = new double[SQRT_LO_TAB_SIZE];
    final double[] sqrtSlopeHiTab = new double[MAX_DOUBLE_EXPONENT-MIN_DOUBLE_EXPONENT+1];
    final double[] sqrtSlopeLoTab = new double[SQRT_LO_TAB_SIZE];
    final double HYPOT_MAX_MAG = twoPow(511);
    final double HYPOT_FACTOR = twoPow(750);

     {
        for (int i=MIN_DOUBLE_EXPONENT;i<=MAX_DOUBLE_EXPONENT;i++) {
            double twoPowExpDiv2 = StrictMath.pow(2.0,i*0.5);
            sqrtXSqrtHiTab[i-MIN_DOUBLE_EXPONENT] = twoPowExpDiv2 * 0.5; // Half sqrt, to avoid overflows.
            sqrtSlopeHiTab[i-MIN_DOUBLE_EXPONENT] = 1/twoPowExpDiv2;
        }
        sqrtXSqrtLoTab[0] = 1.0;
        sqrtSlopeLoTab[0] = 1.0;
        final long SQRT_LO_MASK = (0x3FF0000000000000L | (0x000FFFFFFFFFFFFFL>>SQRT_LO_BITS));
        for (int i=1;i<SQRT_LO_TAB_SIZE;i++) {
            long xBits = SQRT_LO_MASK | (((long)(i-1))<<(52-SQRT_LO_BITS));
            double sqrtX = StrictMath.sqrt(Double.longBitsToDouble(xBits));
            sqrtXSqrtLoTab[i] = sqrtX;
            sqrtSlopeLoTab[i] = 1/sqrtX;
        }
    }

    public double twoPow(int power) {
        if (power <= -MAX_DOUBLE_EXPONENT) {
            if (power >= MIN_DOUBLE_EXPONENT) {
                return Double.longBitsToDouble(0x0008000000000000L>>(-(power+MAX_DOUBLE_EXPONENT)));
            } else {
                return 0.0;
            }
        } else if (power > MAX_DOUBLE_EXPONENT) {
            return Double.POSITIVE_INFINITY;
        } else {
            return Double.longBitsToDouble(((long)(power+MAX_DOUBLE_EXPONENT))<<52);
        }
    }

    double hypot_NaN(double xAbs, double yAbs) {
        if ((xAbs == Double.POSITIVE_INFINITY) || (yAbs == Double.POSITIVE_INFINITY)) {
            return Double.POSITIVE_INFINITY;
        } else {
            return Double.NaN;
        }
    }

    public double hypot(double x, double y) {
        x = Math.abs(x);
        y = Math.abs(y);
        if (y < x) {
            double a = x;
            x = y;
            y = a;
        } else if (!(y >= x)) {
            return hypot_NaN(x, y);
        }

        if (y-x == y) {
            return y;
        } else {
            double factor;
            if (y > HYPOT_MAX_MAG) {
                x *= (1/HYPOT_FACTOR);
                y *= (1/HYPOT_FACTOR);
                factor = HYPOT_FACTOR;
            } else if (x < (1/HYPOT_MAX_MAG)) {
                x *= HYPOT_FACTOR;
                y *= HYPOT_FACTOR;
                factor = (1/HYPOT_FACTOR);
            } else {
                factor = 1.0;
            }
            return factor * Math.sqrt(x*x+y*y);
        }
    }

	protected float[] calculateMagnitudes() {
		return this.fftOut;
	}
}