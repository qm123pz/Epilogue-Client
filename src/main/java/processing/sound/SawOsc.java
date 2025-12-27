package processing.sound;

import com.jsyn.unitgen.SawtoothOscillator;


/**
 * This is a simple Saw Wave Oscillator.
 * @webref Oscillators:SawOsc
 * @webBrief This is a simple Saw Wave Oscillator.
 **/
public class SawOsc extends Oscillator<SawtoothOscillator> {

	/**
	 */
	public SawOsc() {
		super(new SawtoothOscillator());
	}

}
