package org.craigl.softsynth;

/**
* Digital equivalent of a Voltage Controller Amplifier or VCA.
* <p>
* VCA is meant to be driven by an Envelope Generator which controls<br>
* the gain throught the amplifier.
* <p>
* See text for details.
* 
* @author craiglindley
*/
public class VCA extends EnvelopeGenerator implements SampleProviderIntfc {
	
	int noteDex;
	
	/**
	 * VCA Class Constructor
	 * <p>
	 * Creates an VCA instance and initializes it to default values
	 */
	public VCA() {
		// Set envelope generator to reasonable values
		setAttackTimeInMS(1);
		setDecayTimeInMS(1000);
		setSustainLevel(0.5);
		setReleaseTimeInMS(2000);
		
		noteDex = 0;
	}
	
	/**
	 * Setup the provider of samples
	 * 
	 * @param provider The provider of samples for the VCA.
	 */
	public void setSampleProvider(SampleProviderIntfc provider) {
		this.provider = provider;
	}
	
	public void setNote(int i){
		noteDex = i;
	}
	
	/**
	 * Process a buffer full of samples pulled from the sample provider
	 * 
	 * @param buffer Buffer in which the samples are to be processed
	 * 
	 * @return Count of number of bytes processed
	 */
	public int getSamples(byte [][] buffer, int noteDex) {
		
		// Grab samples to manipulate from this module's sample provider
		provider.getSamples(buffer, noteDex);
		
		byte b2 = 0;
		byte b1 = 0;
		short s = 0;
		
		//For each dt in the buffer
		for (int i = 0; i < SamplePlayer.SAMPLES_PER_BUFFER*2; i+=2) {
			//For each note, process the samples
			for(int j = 0; j<BasicOscillator.NUM_NOTES; j++){
				// Get a sample to process
				b2 = buffer[j][i];
				b1 = buffer[j][i+1];
	
				// Convert bytes into short sample
				s=(short)( ((b2&0xFF)<<8) | (b1&0xFF) );
				
				// Apply envelope value to sample
				s *= getValue(j);
				
				// Store the processed sample
				buffer[j][i] = (byte)(s >> 8);
				buffer[j][i+1] = (byte)(s & 0xFF);
			}
		}
		return SamplePlayer.BUFFER_SIZE;
	}
	
	// Instance data
	private SampleProviderIntfc provider;
}
