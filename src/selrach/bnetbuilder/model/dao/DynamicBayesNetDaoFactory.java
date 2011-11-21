package selrach.bnetbuilder.model.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import selrach.bnetbuilder.model.DynamicBNIFExtendedDynamicBayesNetDao;

/**
 * Manager for the different ways to load up a file, this should be modified if
 * there are more file formats created
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class DynamicBayesNetDaoFactory {
	static private final Map<String, DynamicBayesNetDao> daos = new HashMap<String, DynamicBayesNetDao>();
	static {
		DynamicBayesNetDao dao;
		daos.put((dao = new DynamicBNIFExtendedDynamicBayesNetDao()).getName(),
				dao);
	}

	public static List<String> getDaoNameList() {
		return Collections
				.unmodifiableList(new ArrayList<String>(daos.keySet()));
	}

	public static DynamicBayesNetDao getDao(String name) {
		return daos.get(name);
	}

	public static List<DynamicBayesNetDao> getDaoList() {
		return Collections.unmodifiableList(new ArrayList<DynamicBayesNetDao>(
				daos.values()));
	}
}
