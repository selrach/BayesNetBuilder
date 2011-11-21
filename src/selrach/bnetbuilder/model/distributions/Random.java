package selrach.bnetbuilder.model.distributions;

import java.util.Date;

import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

/**
 * This is a wrapper for the random number generators we will
 * use to do our sampling.  I only wanted to create one
 * MersenneTwister engine because of its overhead, all sampling
 * distributions will use this engine.
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 *
 */
public class Random {

	/**
	 * There should be no instances of this class floating about
	 *
	 */
	private Random(){}
	
	private static MersenneTwister engine = null;
	static
	{
		engine = new MersenneTwister(new Date());
	}
	
	private static Normal normal = null;
	/**
	 * Get a Gaussian sampler
	 * @return
	 */
	public static Normal getNormal()
	{
		if(normal == null)
		{
			normal = new Normal(0, 1, engine);
		}
		return normal;
	}
	
	private static Uniform uniform = null;
	/**
	 * Get a uniform sampler.
	 * @return
	 */
	public static Uniform getUniform()
	{
		if(uniform == null)
		{
			uniform = new Uniform(engine);
		}
		return uniform;
	}
}
