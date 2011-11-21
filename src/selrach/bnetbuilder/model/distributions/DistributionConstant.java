package selrach.bnetbuilder.model.distributions;

/**
 * Naming scheme of the different distributions
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public enum DistributionConstant {

	CONDITIONAL_TABLE("Conditional Table"), GAUSSIAN_MIX("Gaussian Mix"), LINEAR_GAUSSIAN(
			"Linear Gaussian"), LINEAR_GAUSSIAN_MIX("Linear Gaussian Mix"), MULTINOMIAL_SIGMOID(
			"Multinomial Sigmoid"), MULTINOMIAL_SIGMOID_MIX(
			"Multinomial Sigmoid Mix"),

	GAUSSIAN("Gaussian"), SIGMOID("Sigmoid"), TABLE("Table"), ;

	private String displayName;

	private DistributionConstant(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static DistributionConstant getEnum(String name) {
		if (DistributionConstant.valueOf(name.toUpperCase()) != null) {
			return DistributionConstant.valueOf(name.toUpperCase());
		}
		for (DistributionConstant dc : values()) {
			if (dc.displayName.equalsIgnoreCase(name)) {
				return dc;
			}

		}
		return null;
	}
}
