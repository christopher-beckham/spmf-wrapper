package main.java.weka.associations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.UUID;
import java.util.Vector;

import ca.pfv.spmf.gui.CommandProcessor;
import ca.pfv.spmf.gui.Main;
import weka.associations.AbstractAssociator;
import weka.associations.AssociationRules;
import weka.associations.AssociationRulesProducer;
import weka.core.Capabilities;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.WekaException;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;

/** 
* @author Christopher Beckham (cjb60 at students dot waikato dot ac dot nz)
*/
public class SPMFWrapper extends AbstractAssociator implements OptionHandler,
	AssociationRulesProducer, TechnicalInformationHandler {
	
	private static final long serialVersionUID = 8378049648280284137L;
	
	protected int m_algorithm = 0;
	protected String m_params = "0.4";
	
	protected String m_spmfOutput = "";
	
	public SPMFWrapper() {
	    resetOptions();
	}
	
	public void resetOptions() {
		m_algorithm = 0;
		m_params = "0.4";
	}
	
	public static final Tag[] TAGS_SELECTION = {
		new Tag(0, "Apriori"),
		new Tag(1, "Apriori_TID"),
		new Tag(2, "FPGrowth_itemsets"),
		new Tag(3, "Relim"),
		new Tag(4, "Eclat"),
		new Tag(5, "dEclat"),
		new Tag(6, "HMine"),
		new Tag(7, "FIN"),
		new Tag(8, "PrePost"),
		new Tag(9, "LCMFreq"),
		new Tag(10, "AprioriClose"),
		new Tag(11, "DCI_Closed"),
		new Tag(12, "Charm_bitset"),
		new Tag(13, "dCharm_bitset"),
		new Tag(14, "Charm_MFI"),
		new Tag(15, "DefMe"),
		new Tag(16, "Pascal"),
		new Tag(17, "Zart"),
		new Tag(18, "AprioriRare"),
		new Tag(19, "AprioriInverse"),
		new Tag(20, "UApriori"),
		new Tag(21, "Two-Phase"),
		new Tag(22, "FHM"),
		new Tag(23, "HUI-Miner"),
		new Tag(24, "UPGrowth"),
		new Tag(25, "IHUP"),
		new Tag(26, "VME"),
		new Tag(27, "MSApriori"),
		new Tag(28, "CFPGrowth++"),
		new Tag(29, "FPGrowth_association_rules"),
		new Tag(30, "FPGrowth_association_rules_with_lift"),
		new Tag(31, "IGB"),
		new Tag(32, "Sporadic_association_rules"),
		new Tag(33, "Closed_association_rules"),
		new Tag(34, "MNR"),
		new Tag(35, "Indirect_association_rules"),
		new Tag(36, "FHSAR"),
		new Tag(37, "TopKRules"),
		new Tag(38, "TNR"),
	};
	
	public SelectedTag getAlgorithm() {
		return new SelectedTag(m_algorithm, TAGS_SELECTION);
	}
	
	public void setAlgorithm(SelectedTag algorithm) {
		if(algorithm.getTags() == TAGS_SELECTION) {
			this.m_algorithm = algorithm.getSelectedTag().getID();
		}
	}
	
	public String algorithmTipText() {
		return "Algorithm to use (will throw exception if it doesn't exist in SPMF)";
	}
	
	public String getParams() {
		return m_params;
	}
	
	public void setParams(String params) {
		this.m_params = params;
	}
	
	public String paramsTipText() {
		return "Parameters for the algorithm (space-separated if more than one parameter)";
	}
	
	public String globalInfo() {
		return "Wrapper class for SPMF (Sequential Pattern Mining Framework),"
				+ " a data mining library that specialises in frequent pattern mining. "
				+ "For more information, see:\n\n" + getTechnicalInformation().toString();
	}
	
	@Override
	public Capabilities getCapabilities() {
		Capabilities result = super.getCapabilities();
		result.disableAll();
		
		// enable what we can handle
		
		// attributes
		result.enable(Capability.NOMINAL_ATTRIBUTES);
		result.enable(Capability.MISSING_VALUES);
		
		// class (can handle a nominal class if CAR rules are selected). This
		result.enable(Capability.NO_CLASS);
		result.enable(Capability.NOMINAL_CLASS);
		result.enable(Capability.MISSING_CLASS_VALUES);
		
		return result;
	}

	@Override
	public void buildAssociations(Instances data) throws Exception {	
		
		getCapabilities().testWithFail(data);
		
		/*
		 * HACKY: Because SPMF reads input/output files and I don't want to alter
		 * the code and include it with my package, we'll have to be a bit
		 * inefficient and save this arff file temporarily and then load it in
		 * with SPMF and grab the output. Grrrrrrrrrrrr!
		 */
		
		ArffSaver saver = new ArffSaver();
		saver.setInstances(data);
		String randFilename = System.getProperty("java.io.tmpdir") + UUID.randomUUID().toString() + ".arff";
		saver.setFile( new File( randFilename ) );
		saver.writeBatch();
		
		/*
		 * HACKY: If you use an algorithm like CFPGrowth++, you specify an MIS.txt
		 * file after your output file, e.g:
		 * java -jar spmf.jar run CFPGrowth++ contextCFPGrowth.txt output.txt MIS.txt
		 * 
		 * SPMF's code assumes that if you specify an ARFF path that includes a parent
		 * e.g. C:\temp\hello.arff, that the MIS.txt file will therefore be located at
		 * C:\temp\MIS.txt, which is a bit of a weird assumption... Anyways, we have to
		 * deal with it. 
		 * 
		 * Assume each parameter is a file (most params won't be). If one of the params
		 * is actually a file (i.e. it actually *is* a file!), then copy this to the
		 * same temp folder that the ARFF file is in.
		 * 
		 * -t rgd.arff -M CFPGrowth++ -P spmf_inputs/MIS.txt
		 */
		
		String[] splitParams = getParams().split(" ");
		String randParamFilename = null;
		for(int i = 0; i < splitParams.length; i++) {
			if( new File(splitParams[i]).isFile() ) {
				randParamFilename = System.getProperty("java.io.tmpdir") + UUID.randomUUID().toString() + ".txt";
				Files.copy(
					new File(splitParams[i]).toPath(),
					//new File( System.getProperty("java.io.tmpdir") + new File(splitParams[i]).getName() ).toPath(),
					new File(randParamFilename).toPath(),
					StandardCopyOption.REPLACE_EXISTING 
				);
				splitParams[i] = new File(randParamFilename).getName();
			}
		}
		
		String[] cmdString = new String[4 + splitParams.length];
		cmdString[0] = "run";
		//cmdString[1] = getAlgorithm();
		cmdString[1] = TAGS_SELECTION[m_algorithm].getReadable();
		cmdString[2] = randFilename;
		cmdString[3] = randFilename + ".out";
		for(int i = 0; i < splitParams.length; i++) {
			cmdString[4+i] = splitParams[i];
		}
		
		Main.processCommandLineArguments(cmdString);
		
		System.out.println();
		
		StringBuilder sb = new StringBuilder();
		sb.append("Algorithm: " + getAlgorithm().getSelectedTag().getReadable() + "\n");
		sb.append("Parameters: " + getParams() + "\n");
		sb.append("\n");
		
		File outFile = new File(randFilename + ".out");
		FileReader fr = new FileReader(outFile);
		BufferedReader br = new BufferedReader(fr);
		String line;
		while( (line = br.readLine()) != null ) {
			sb.append(line);
			sb.append("\n");
		}
		fr.close();
		m_spmfOutput = sb.toString();
		
		new File(randFilename).deleteOnExit();
		new File(randFilename + ".out").deleteOnExit();
		
	}
	
	@Override
	public String toString() {
		return m_spmfOutput.toString();
	}

	@Override
	public TechnicalInformation getTechnicalInformation() {
		 TechnicalInformation result = new TechnicalInformation(Type.ARTICLE);
		 result.setValue(Field.AUTHOR, "Philippe Fournier-Viger, Antonio Gomariz,"
		 		+ " Ted Gueniche, Azadeh Soltani, Cheng-Wei Wu, Vincent S. Tseng");
		 result.setValue(Field.TITLE, "SPMF: A Java Open-Source Pattern Mining Library");
		 result.setValue(Field.BOOKTITLE, "Journal of Machine Learning Research 15 (2014)");
		 result.setValue(Field.YEAR, "2013");
		 result.setValue(Field.PAGES, "3389-3393");
		 result.setValue(Field.URL, "http://jmlr.org/papers/volume15/fournierviger14a/fournierviger14a.pdf");
		 return result;
	}

	@Override
	public AssociationRules getAssociationRules() {
		return null;
	}

	@Override
	public String[] getRuleMetricNames() {
		return null;
	}

	@Override
	public boolean canProduceRules() {
		return true;
	}

	@Override
	public Enumeration<Option> listOptions() {
	    Vector<Option> newVector = new Vector<Option>(2);
	    newVector.add( new Option("Algorithm to use", "M", 1,
	      "-M <name of algorithm>") );
	    newVector.add( new Option("Parameters for algorithm", "P", 1,
	    		"-P <params>") );
	    return newVector.elements();
	}

	@Override
	public void setOptions(String[] options) throws Exception {

		String method = Utils.getOption('M', options);	
		try {
			/*
			 * If the user provides the algorithm index (e.g. -M 0)
			 * then simply set the algorithm to that index and then
			 * we're done.
			 */
			int num = Integer.parseInt(method);
			setAlgorithm( new SelectedTag(num, TAGS_SELECTION) );				
		} catch (NumberFormatException ex) {
			/*
			 * If the user provides the algorithm name (e.g. -M Apriori)
			 * then find what index it corresponds to in TAGS_SELECTION
			 * and then call setAlgorithm(), passing the algorithm's index
			 * (e.g. 0)
			 */				
			if(method.length() == 0) {
				setAlgorithm( new SelectedTag(0, TAGS_SELECTION) );
			} else {				
				boolean foundAlgorithm = false;
				for(int i = 0; i < TAGS_SELECTION.length; i++) {
					if(method.equals(TAGS_SELECTION[i].getReadable())) {
						setAlgorithm( new SelectedTag(i, TAGS_SELECTION) );
						foundAlgorithm = true;
						break;
					}
				}
				if(foundAlgorithm == false) {
					setAlgorithm( new SelectedTag(0, TAGS_SELECTION) );
				}
			}
		}
		
		String params = Utils.getOption('P', options);

		setParams(params);	
		
	}

	@Override
	public String[] getOptions() {
		String[] options = new String[4];
		int current = 0;
		options[current++] = "-M";
		options[current++] = getAlgorithm().getSelectedTag().getReadable();
		options[current++] = "-P";
		options[current++] = getParams();
		return options;
	}
	
	public static void main(String[] args) {
		AbstractAssociator.runAssociator( new SPMFWrapper(), args );
	}

}
