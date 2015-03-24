/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 */

package weka.associations;

import java.util.HashMap;
import java.util.HashSet;

import weka.associations.AbstractAssociatorTest;
import weka.associations.Associator;
import weka.associations.SPMFWrapper;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import junit.framework.Test;
import junit.framework.TestSuite;
import weka.core.converters.ConverterUtils.DataSource;

/**
 * Tests Apriori. Run from the command line with:<p/>
 * java weka.associations.AprioriTest
 *
 * @author Christopher Beckham (cjb60 at students dot waikato dot ac dot nz)
 * @version $Revision: 8034 $
 */
public class SPMFWrapperTest 
  extends AbstractAssociatorTest {

  public SPMFWrapperTest(String name) { 
    super(name);  
  }
  

  public void testAll() {
	  
	  HashMap<String, String[]> algorithmToParam = new HashMap<String, String[]>();
	  algorithmToParam.put("DCI_Closed", new String[] { "contextPasquier99.arff", "2" });
	  algorithmToParam.put("AprioriInverse", new String[] { "contextPasquier99.arff", "0.1 0.6" });
	  //algorithmToParam.put("UApriori", new String[] { "contextUncertain.arff", "0.1" });
	  algorithmToParam.put("Two-Phase", new String[] { "contextPasquier99.arff", "30" });
	  algorithmToParam.put("VME", new String[] { "contextPasquier99.arff", "15%" });
	  algorithmToParam.put("MSApriori", new String[] { "contextPasquier99.arff", "0.4 0.2" });
	  algorithmToParam.put("CFPGrowth++", new String[] { "contextPasquier99.arff",
			  this.getClass().getResource("MIS.txt").getPath() } );
	  algorithmToParam.put("FPGrowth_association_rules", new String[] { "contextPasquier99.arff", "0.5 0.6" });
	  algorithmToParam.put("FPGrowth_association_rules_with_lift", new String[] { "contextPasquier99.arff", "0.5 0.9 1" });
	  algorithmToParam.put("IGB", new String[] { "contextPasquier99.arff", "0.5 0.6" });
	  algorithmToParam.put("Sporadic_association_rules", new String[] { "contextPasquier99.arff", "1 0.6 0.6" });
	  algorithmToParam.put("Closed_association_rules", new String[] { "contextPasquier99.arff", "0.6 0.6" });
	  algorithmToParam.put("MNR", new String[] { "contextPasquier99.arff", "0.6 0.6" });
	  algorithmToParam.put("Indirect_association_rules", new String[] { "contextPasquier99.arff", "0.6 0.5 0.1" });
	  algorithmToParam.put("FHSAR", new String[] { "contextPasquier99.arff", "0.5 0.6 " + this.getClass().getResource("sar.txt").getPath() });
	  algorithmToParam.put("TopKRules", new String[] { "contextPasquier99.arff", "2 0.8" });
	  algorithmToParam.put("TNR", new String[] { "contextPasquier99.arff", "30 0.5 2" });
	  
	  HashSet<String> ignoreAlgorithm = new HashSet<String>();
	  ignoreAlgorithm.add("HMine"); // doesn't work, even for spmf.jar
	  ignoreAlgorithm.add("DCI_Closed"); // doesn't like the spmf file that gets converted from arff
	  ignoreAlgorithm.add("UApriori"); // takes probabilities for items, but doesn't like a numeric arff (which you need)
	  ignoreAlgorithm.add("Two-Phase"); // takes a special type of input that doesn't appear it could be converted to arff
	  ignoreAlgorithm.add("FHM"); // same as above
	  ignoreAlgorithm.add("HUI-Miner"); // same as above
	  ignoreAlgorithm.add("UPGrowth"); // same as above
	  ignoreAlgorithm.add("IHUP"); // same as above
	  
	  SPMFWrapper wrapper = new SPMFWrapper();
	  
	  // iterate through all the algorithms
	  for(int i = 0; i < wrapper.TAGS_SELECTION.length; i++) {	  
		  String algName = wrapper.TAGS_SELECTION[i].getReadable();
		  try {
			if(ignoreAlgorithm.contains(algName)) {
				continue; // these algorithms don't work in spmf, even
			}
			String defaultParam = "0.4";
			String defaultDataset = "contextPasquier99.arff";
			if( algorithmToParam.containsKey(algName) ) {
				defaultDataset = algorithmToParam.get(algName)[0];
				defaultParam = algorithmToParam.get(algName)[1];
			}
			wrapper.setOptions(new String[] { "-M", algName, "-P", defaultParam } );
			DataSource source = new DataSource(this.getClass().getResourceAsStream(defaultDataset));
			Instances data = source.getDataSet();
			System.out.println("algorithm="+algName);
			System.out.println("options=");
			for(String s : wrapper.getOptions()) {
				System.out.println(s);
			}
			wrapper.buildAssociations(data);
		  } catch (Exception e) {
			e.printStackTrace();
			fail();
			System.exit(1);
		  }
	  }
	  
  }


  /** Creates a default Apriori */
  public Associator getAssociator() {
    return new SPMFWrapper();
  }

  public static Test suite() {
    return new TestSuite(SPMFWrapperTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
