package processing.sound;

import com.jsyn.ports.UnitOutputPort;


/**
 * Detects the pitch (also known as the 'fundamental frequency') of a processing.sound
 * signal. For complex signals this is not a trivial task, so the analyzer only 
 * returns a frequency measurement (measured in Hertz) when its measurement 
 * exceeds a 'confidence level' that can be specified by the user.
 *
 * @webref Analysis:PitchDetector
 * @webBrief Detects the fundamental frequency of a processing.sound signal
 */
public class PitchDetector extends Analyzer {
  private final com.jsyn.unitgen.PitchDetector detector;

  private float minimumConfidence;

  /**
   * @param minimumConfidence the minimum confidence level required for
   * frequency measurements, between <code>0.0</code> (accept all measurements, 
   * no matter how unreliable) to <code>1.0</code> (only accept perfect 
   * measurements). Defaults to 0.8.
   */
  public PitchDetector(float minimumConfidence) {
    super();
    this.detector = new com.jsyn.unitgen.PitchDetector();
    this.minimumConfidence = minimumConfidence;
  }

  public PitchDetector() {
    this(0.8f);
  }

  @Override
  public void removeInput() {
    this.input = null;
  }

  @Override
  protected void setInput(UnitOutputPort input) {
    Engine.getEngine().add(this.detector);
    this.detector.start();
    this.detector.input.connect(input);
  }

  public float analyze() {
    return this.analyze(this.minimumConfidence);
  }

  /**
   * Returns an estimate of the current pitch (or 'fundamental frequency') of 
   * the input processing.sound signal, in Hertz. If the confidence in the current
   * measurement does not exceed the minimum confidence, this method returns 
   * <code>0.0</code>.
   * @webref Analysis:PitchDetector
   * @webBrief Detect the fundamental frequency of the input processing.sound signal.
   * @param minimumConfidence the minimum confidence level required for 
   * frequency measurements, between 0 (accept all measurements, no matter how 
   * unreliable) to 1 (only accept perfect measurements). If omitted, uses the 
   * confidence level specified when this PitchDetector was created.
   * @param target a float array of length 2 that will be filled with the 
   * frequency and confidence in that frequency measurement
   */
  public float analyze(float minimumConfidence) {
    return (float) (this.detector.confidence.getValue() >= minimumConfidence ? this.detector.frequency.getValue() : 0.0);
  }

  public float analyze(float[] target) {
    target[0] = (float) this.detector.frequency.getValue();
    target[1] = (float) this.detector.confidence.getValue();
    return (target[1] >= this.minimumConfidence ? target[0] : 0.0f);
  }
}
