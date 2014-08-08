package org.craigl.softsynth;

/**
 * Envelope Generator
 * <p>
 * Sometime called an ADSR for attack, decay, sustain and release
 * <p>
 * This component produces a time varying value between 0.0 and 1.0<br>
 * with controlled attack slope, decay slope, sustain value and release slope.
 * <p>
 * This components getValue method must be called at the sample rate<br>
 * as all timing is derived from the sample timing.
 * <p>
 * This component is built as a state machine. See text for details.
 * 
 * @author craiglindley
 */

public class EnvelopeGenerator {
	
	// Parameter ranges
	public static final int MS_MIN = 1;
	public static final int MS_MAX = 5000;
	public static final double SUSTAIN_MIN = 0.0;
	public static final double SUSTAIN_MAX = 1.0;
	
	// Instance data
	//Where do you set the noteDex?
	public boolean noteOn[] = new boolean[BasicOscillator.NUM_NOTES];
	public boolean noteOff[] = new boolean[BasicOscillator.NUM_NOTES];
	private int count[] = new int[BasicOscillator.NUM_NOTES];
	public SM_STATE state[] = new SM_STATE[BasicOscillator.NUM_NOTES];;
	private double sustainLevel;
	private double sampleTime;
	private int attackCount;
	private double attackSlope;
	private int decayMS;
	private int decayCount;
	private double decaySlope;
	private int releaseMS;
	private int releaseCount;
	private double releaseSlope;
	
	//private int noteDex;
	//static final int numNotes = 3;
	
	// States of the Envelope Generator
	public enum SM_STATE {
		STATE_IDLE, STATE_ATTACK, STATE_DECAY, STATE_SUSTAIN, STATE_RELEASE
	}
	
	/**
	 * EnvelopeGenerator Class Constructor
	 * <p>
	 * EnvelopeGenerator instance is initialized and placed into the idle state<br>
	 * awaiting a noteOn event to begin operation.
	 */
	public EnvelopeGenerator() {
		
		for(int i=0;i<BasicOscillator.NUM_NOTES;i++){
			noteOn[i] = false;
			noteOff[i] = false;
			count[i] = 0;
			state[i] = SM_STATE.STATE_IDLE;
		}
		
		sustainLevel = 0.0;

		// Calculate sample time
		sampleTime = (1.0 / SamplePlayer.SAMPLE_RATE);
	}
	
	/**
	 * This method is called to initiate the envelope generation process.<br>
	 * The state machine transitions through the attack, decay and sustain<br>
	 * states then awaits the noteOff event.
	 */
	public void noteOn(int i) {
		//System.out.println("on: "+i);
		noteOn[i] = true;
	}

	/**
	 * A noteOff event completes the envelope generation process and returns<br>
	 * the EnvelopeGenerator to the idle state awaiting the next noteOn event.
	 */
	public void noteOff(int i) {
		//System.out.println("off: "+i);
		noteOff[i] = true;
	}

	/**
	 * Sets the attack time of the generated envelope. This is the time<br>
	 * for the envelope value to go from 0.0 to 1.0.
	 * <p>
	 * Valid attack times MS_MIN <= attackTime <= MS_MAX
	 * 
	 * @param ms The attack time in milliseconds 
	 */
	public void setAttackTimeInMS(int ms) {
		
		// Range check incoming value
		ms = (ms < MS_MIN) ? MS_MIN : ms;
		ms = (ms > MS_MAX) ? MS_MAX : ms;
		
		double temp = ((0.001 * ms) / sampleTime);
		attackCount = (int) temp;
		attackSlope = (1.0 / temp);
	}

	/**
	 * Sets the decay time of the generated envelope. This is the time<br>
	 * for the envelope value to go from 1.0 to the sustain level.
	 * <p>
	 * Valid decay times MS_MIN <= decayTime <= MS_MAX
	 * 
	 * @param ms The decay time in milliseconds 
	 */
	public void setDecayTimeInMS(int ms) {
		
		// Range check incoming value
		ms = (ms < MS_MIN) ? MS_MIN : ms;
		ms = (ms > MS_MAX) ? MS_MAX : ms;

		decayMS = ms;
		double temp = ((0.001 * ms) / sampleTime);
		decayCount = (int) temp;
		decaySlope = ((1.0 - sustainLevel) / temp);
	}

	/**
	 * Sets the sustain level of the generated envelope.
	 * <p>
	 * The sustain portion of the envelope is between the end<br>
	 * of the decay interval and the noteOff event.
	 * <p>
	 * Valid sustain values SUSTAIN_MIN <= sustain value <= SUSTAIN_MAX<br>
	 * or between 0.0 and 1.0. 
	 * 
	 * @param level The sustain level to produce
	 */	
	public void setSustainLevel(double level) {
 
		// Range check incoming value
		sustainLevel = (level < SUSTAIN_MIN) ? SUSTAIN_MIN : level;
		sustainLevel = (level > SUSTAIN_MAX) ? SUSTAIN_MAX : level;
		
		// Recalculate decay and release times accordingly
		setDecayTimeInMS(decayMS);
		setReleaseTimeInMS(releaseMS);
	}

	/**
	 * Sets the release time of the generated envelope. This is the time<br>
	 * for the envelope value to go from the sustain level to 0.0.
	 * <p>
	 * Valid release times MS_MIN <= releaseTime <= MS_MAX
	 * 
	 * @param ms The release time in milliseconds 
	 */	
	public void setReleaseTimeInMS(int ms) {
		
		// Range check incoming value
		ms = (ms < MS_MIN) ? MS_MIN : ms;
		ms = (ms > MS_MAX) ? MS_MAX : ms;

		releaseMS = ms;
		double temp = ((0.001 * ms) / sampleTime);
		releaseCount = (int) temp;
		releaseSlope = (sustainLevel / temp);
	}

	/**
	 * Run the envelope generator state machine to return the next value
	 * <p>
	 * This method must be called each sample time to maintain accurate timing.
	 * 
	 * @return The envelope value between 0.0 and 1.0
	 */ 
	public double getValue(int noteDex) {
		
		double value = 0.0;
		
		switch (state[noteDex]) {
			// Process the idle state
			case STATE_IDLE:
				
				if (noteOn[noteDex]) {
					noteOff[noteDex] = false;
					noteOn[noteDex] = false;
					count[noteDex] = 0;
					state[noteDex] = SM_STATE.STATE_ATTACK;
				}
				break;
				
			// Process the attack state
			case STATE_ATTACK:
				// Did another noteOn event occur?
				if (noteOn[noteDex]) {
					state[noteDex] = SM_STATE.STATE_IDLE;
					break;
				}
				// Calculate the value to return
				value = count[noteDex] * attackSlope;

				// Has attack time elapsed ?
				if (count[noteDex] >= attackCount) {
					count[noteDex] = 0;
					state[noteDex] = SM_STATE.STATE_DECAY;				
				}	else	{
					count[noteDex]++;
				}
				break;
				
			// Process the decay state
			case STATE_DECAY:
				// Did another noteOn event occur?
				if (noteOn[noteDex]) {
					state[noteDex] = SM_STATE.STATE_IDLE;
					break;
				}
				// Calculate the value to return
				value = 1.0 - (count[noteDex] * decaySlope);

				// Has decay time elapsed ?
				if (count[noteDex] >= decayCount) {
					state[noteDex] = SM_STATE.STATE_SUSTAIN;
				}	else	{
					count[noteDex]++;
				}
				break;
				
			// Process the sustain state
			case STATE_SUSTAIN:
				// Did another noteOn event occur?
				if (noteOn[noteDex]) {
					state[noteDex] = SM_STATE.STATE_IDLE;
					break;
				}
				// Get value to return
				value = sustainLevel;

				// Did a noteOff event occur ?
				if (noteOff[noteDex]) {
					//System.out.println("Off:"+noteDex);
					noteOff[noteDex] = false;
					count[noteDex] = 0;
					state[noteDex] = SM_STATE.STATE_RELEASE;
				}
				break;
				
			// Process the release state
			case STATE_RELEASE:
				// Did another noteOn event occur?
				if (noteOn[noteDex]) {
					state[noteDex] = SM_STATE.STATE_IDLE;
					break;
				}
				// Calculate the value to return
				value = sustainLevel - (count[noteDex] * releaseSlope);
				if (value < 0) {
					value = 0;
				}

				// Has release time elapsed ?
				if (count[noteDex] >= releaseCount) {
					state[noteDex] = SM_STATE.STATE_IDLE;
				}	else	{
					count[noteDex]++;
				}
				break;
		}
		return value;
	}
	
	//If the note is currently producing sound
	public boolean noteIsIdle(int i){
		return (state[i] == SM_STATE.STATE_IDLE);
	}
	
}
