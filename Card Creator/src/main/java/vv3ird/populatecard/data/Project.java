package vv3ird.populatecard.data;

import java.awt.Font;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.csv.CSVFormat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import vv3ird.populatecard.control.ProjectManager;

/**
 * Project that contains all necessary information to create cards<br>
 * Project structure (As created by {@link ProjectManager}:<br><br>
 * <code>projects/{@link #name}/{@link #name}.cmp<br>
 * projects/{@link #name}/csv/<br>
 * projects/{@link #name}/fonts/<br></code>
 * @author VV3IRD
 *
 */
public class Project {
	
	/**
	 * Loads a project from a given path
	 * @param path	Project file to be loaded
	 * @return The project object
	 * @throws IOException	Whenever the project file cannot be loaded from the given file.
	 */
	public static Project load(Path path) throws IOException {
		Project project = null;
		if (path.toString().endsWith(".cmpz"))
			project = loadCompressed(path);
		else {
			byte[] projectBytes = Files.readAllBytes(path);
			String projectString = new String(projectBytes, "UTF-8");
			Gson gson = new Gson();
			project = gson.fromJson(projectString, Project.class);
		}
		if (project.fp != null)
			project.fp.decodeImages();
		project.init();
		project.setProjectRoot(path.getParent());
		return project;
	}
	
	private static Project loadCompressed(Path path) throws IOException {
		Project p = null;
		Gson g = new Gson();
		InputStream input = Files.newInputStream(path);
		try {
			Reader reader = new InputStreamReader(new GZIPInputStream(input), StandardCharsets.UTF_8);
			try {
				p = g.fromJson(reader, Project.class);
			} finally {
				reader.close();
			}
		} finally {
			input.close();
		}
		return p;
	}

	/**
	 * Save a project to the given file. Project is transformed to a Json String and then saved to disk.
	 * @param project	Project that should be saved
	 * @param path		Project file
	 * @throws IOException	Whenever an IO Error occurs while writing data to disk.
	 */
	public static void save(Project project, Path path, boolean keepBackup) throws IOException {
		if (project.fp != null)
			project.fp.encodeImages();
		String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(project);
		if(path.toString().endsWith(".cmpz"))
			saveCompressed(jsonString, path, keepBackup);
		else
			saveUncompressed(jsonString, path, keepBackup);
	}
	
	private static void saveUncompressed(String jsonString, Path path, boolean keepBackup) throws IOException {
		try {
			byte[] utf8JsonString = jsonString.getBytes("UTF-8");
			if (Files.exists(path))
				Files.copy(path, path.getParent().resolve("." + path.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
			Files.write(path, utf8JsonString, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (UnsupportedEncodingException e) {
		}
	}

	private static void saveCompressed(String jsonString, Path path, boolean keepBackup) throws IOException {
		if (Files.exists(path))
			Files.copy(path, path.getParent().resolve("." + path.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
		FileOutputStream output = new FileOutputStream(path.toFile());
		try {
			Writer writer = new OutputStreamWriter(new GZIPOutputStream(output), StandardCharsets.UTF_8);
			try {
				writer.write(jsonString);
			} finally {
				writer.close();
			}
		} finally {
			output.close();
		}
	}

	/**
	 * CSV Data, will be loaded by {@link ProjectManager} when CSV exists in project folder
	 */
	private transient String[][] csvData = null;
	
	/**
	 * Mappings {@link Field} names to CSV columns 
	 */
	private Map<String, String> csvFieldMapping = null;
	
	/**
	 * Map with column header names to column index, will be loaded by {@link ProjectManager} when CSV exists in project folder
	 */
	private transient Map<String, Integer> csvHeader = null;
	
	/**
	 * Template for generated cards.<br>
	 * {no} - Number of card created<br>
	 * {side} - side of the card (front or rear)<br>
	 * + Column headers. 
	 */
	private String fileNameTemplate = "{no}.png";
	
	/**
	 * Available custom fonts for this Project
	 */
	private transient Map<String, Font> fonts = null;
	
	/**
	 * Field package that contains image data and {@link Field}s
	 */
	private FieldPackage fp = null;
	
	/**
	 * Name of the project
	 */
	private String name = null;
	
	/**
	 * Root folder that will be populated when the project is loaded from file by {@link ProjectManager}
	 */
	private transient Path projectRoot = null;

	/**
	 * CSV-Delimiter for the csv columns
	 */
	private char csvDelimiter = ';';
	
	/**
	 * Quotationsmarks for the csv files
	 */
	private char csvQuote = '"';
	
	/**
	 * Record separator for the csv file
	 */
	private String csvRecordSeparator = "\n";
	
	/**
	 * Determines the amount of threads used for processing 
	 */
	private ParallelProcessing processingMode = ParallelProcessing.CPU_MINUS_ONE;
	
	/**
	 * Custom processing thread value
	 */
	private int customParallelProcessingThreads = 1;
		
	
	/**
	 * Creates a new project with only a name.
	 * @param name
	 */
	public Project(String name) {
		this.name = name;
		this.fp = new FieldPackage();
		this.csvFieldMapping = new HashMap<>();
		this.fonts = new HashMap<>();
	}
	
	/**
	 * Adds a font to the project object
	 * @param fontName	Name of the font.
	 * @param font		Font object
	 */
	public void addFont(String fontName, Font font) {
		this.fonts.put(fontName, font);
	}
	
	/**
	 * Maps a certain {@link Field} to a CSV column.
	 * @param field		Fieldname to be mapped
	 * @param csvColumn	CVS-Columnname to be mapped
	 */
	public void addMapping(String field, String csvColumn) {
		this.csvFieldMapping.put(field, csvColumn);
	}
	
	/**
	 * Returns the columnname mapped to the {@link Field}name
	 * @param field	{@link Field}name the column should be retrieved for
	 * @return	Columnname or null, if no mapping exists.
	 */
	public String getCsvColumn(String field) {
		return this.csvFieldMapping.get(field);
	}
	
	public int getMappedCsvColumnIndex(String field) {
		return this.csvHeader.get(this.csvFieldMapping.get(field));
	}
	
	public int getCsvColumnIndex(String csvHeader) {
		return this.csvHeader.get(csvHeader);
	}
	
	public String[][] getCsvData() {
		return csvData;
	}
	
	public Map<String, Integer>  getCsvHeader() {
		return csvHeader != null ? new TreeMap<>(csvHeader) : null;
	}
	
	public String getFileNameTemplate() {
		return fileNameTemplate;
	}
	
	public Map<String, Font> getFonts() {
		return fonts;
	}

	public FieldPackage getFp() {
		return fp;
	}

	public List<String> getMappedFields () {
		return new ArrayList<>(this.csvFieldMapping.keySet());
	}
	
	
	public Map<String, String> getFieldMappings() {
		return new HashMap<>(this.csvFieldMapping);
	}
	
	public String getName() {
		return name;
	}

	public Path getProjectRoot() {
		return projectRoot;
	}

	private void init() {
		this.fonts = new HashMap<>();
		if(this.csvFieldMapping == null) {
			csvFieldMapping = new HashMap<>();
		}
		if (this.csvQuote == this.csvDelimiter ) {
			this.csvQuote = '"';
			this.csvDelimiter = ';';
		}
		if (this.csvRecordSeparator == null )
			this.csvRecordSeparator = "\n";
	}

	public void removeFont(String fontName) {
		this.fonts.remove(fontName);
	}
	
	public void setCsvData(String[][] csvData) {
		this.csvData = csvData;
	}

	public void setCsvHeader(Map<String, Integer> csvHeader) {
		this.csvHeader = new TreeMap<>(csvHeader);
	}
	
	public void setFileNameTemplate(String fileNameTemplate) {
		this.fileNameTemplate = fileNameTemplate;
	}
	
	public void setFp(FieldPackage fp) {
		this.fp = fp;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	private void setProjectRoot(Path projectRoot) {
		this.projectRoot = projectRoot;
	}
	
	/**
	 * Verifies and removes invalid Field to CSV column mappings
	 * @param remove <code>true</code> if invalid mappings should be removed, <code>false</code> if only the number of invalid mappings should be calculated.
	 * @return Number of mappings that were invalid and got removed.
	 */
	public int verifyFieldMapping(boolean remove) {
		List<String> mtbr = new LinkedList<>();
		for (String fieldName : csvFieldMapping.keySet()) {
			if (this.fp.getFieldByName(fieldName) == null) {
				mtbr.add(fieldName);
			}
			else if (csvHeader != null && csvHeader.get(csvFieldMapping.get(fieldName)) == null) {
				mtbr.add(fieldName);
			}
		}
		if(remove) {
			for (String fn : mtbr) {
				this.csvFieldMapping.remove(fn);
			}
		}
		return mtbr.size();
	}

	public Font getFont(String font) {
		return this.fonts.get(font);
	}
	
	public CSVFormat getCSVFormat() {
		return CSVFormat.newFormat(this.csvDelimiter).withQuote(this.csvQuote).withRecordSeparator(this.csvRecordSeparator).withFirstRecordAsHeader();
	}
	
	public char getCsvDelimiter() {
		return csvDelimiter;
	}
	
	public Map<String, String> getCsvFieldMapping() {
		return csvFieldMapping;
	}
	
	public char getCsvQuote() {
		return csvQuote;
	}
	
	public String getCsvRecordSeparator() {
		return csvRecordSeparator;
	}
	
	public void setCsvDelimiter(char csvDelimiter) {
		this.csvDelimiter = csvDelimiter;
	}
	
	public void setCsvFieldMapping(Map<String, String> csvFieldMapping) {
		this.csvFieldMapping = csvFieldMapping;
	}
	
	public void setCsvQuote(char csvQuote) {
		this.csvQuote = csvQuote;
	}
	
	public void setCsvRecordSeparator(String csvRecordSeparator) {
		this.csvRecordSeparator = csvRecordSeparator;
	}

	public boolean hasCsvData() {
		return this.csvData != null;
	}
	
	
	public int getCustomParallelProcessingThreads() {
		return customParallelProcessingThreads;
	}
	
	public void setCustomParallelProcessingThreads(int customParallelProcessingThreads) {
		this.customParallelProcessingThreads = customParallelProcessingThreads;
	}
	
	public ParallelProcessing getProcessingMode() {
		if(processingMode == null)
			processingMode = ParallelProcessing.CPU_MINUS_ONE;
		return processingMode;
	}
	
	public void setProcessingMode(ParallelProcessing processingMode) {
		this.processingMode = processingMode;
	}

	public int getParallelProcessingThreads() {
		return this.processingMode == ParallelProcessing.CPU_MINUS_ONE ? Runtime.getRuntime().availableProcessors()-1 : this.processingMode == ParallelProcessing.SINGLE_THREAD ? 1 : this.customParallelProcessingThreads;
	}

	public Field getFieldByName(String fieldName) {
		return fp.getFieldByName(fieldName);
	}

	public List<Field> getCalculatedFields() {
		return fp.getCalculatedFields();
	}
}
