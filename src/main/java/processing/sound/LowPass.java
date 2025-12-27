package processing.sound;

import com.jsyn.unitgen.FilterLowPass;

/**
 * This is a low pass filter.
 * @webref Effects:LowPass
 **/
public class LowPass extends Filter<FilterLowPass> {

	public LowPass() {
		super();
	}

	@Override
	protected FilterLowPass newInstance() {
		return new FilterLowPass();
	}
}
