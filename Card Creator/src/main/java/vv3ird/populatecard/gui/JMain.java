package vv3ird.populatecard.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.csv.CSVParser;

import vv3ird.populatecard.CardCreator;
import vv3ird.populatecard.control.ProjectManager;
import vv3ird.populatecard.control.TaskScheduler;
import vv3ird.populatecard.control.postprocessing.ReplaceImage;
import vv3ird.populatecard.data.Field;
import vv3ird.populatecard.data.FieldPackage;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.border.BevelBorder;
import javax.swing.BoxLayout;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Desktop;

import javax.swing.Box;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class JMain extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8050980040476237624L;
	private JPanel contentPane;
	private JMenu mnRecentProjects;

	private JMenu mnDeleteFont;
	private JMenu mnFonts;

	private BufferedImage frontImage;
	private BufferedImage rearImage;
	private BufferedImage alternateRearImage;
	private JButton btnLoadFrontSide;
	private JButton btnLoadRearSide;
	private JLabel pnFrontPreview;
	private JLabel pnRearPreview;
	private JPanel pnStatus;
	private JStatusLabel lblStatus;
	private JMenuItem mntmMapFields;
	private JMenuItem mntmImportCmFile;
	private JMenuItem mntmImportFont;
	private JTextField tfFileNameTemplate;
	private JButton btnI;
	private JButton btnCreateCards;
	private JLabel pnAlternateRearPreview;
	private JButton btnLoadAlternateRear;
	private JMenuItem mntmExportProject;
	private JMenuItem mntmImportCsv;
	private JMenuItem mntmImportCsv2;
	private JMenuItem mntmSaveProject;
	private JMenuItem mntmMapCsv;
	private JMenuItem mntmConfiguration;

	/**
	 * Create the frame.
	 */
	public JMain() {
		setTitle("Card Creator");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 482, 450);
		setResizable(false);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenuItem mntmNewProject = new JMenuItem("New project");
		mntmNewProject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				boolean ex = false;
				String name = null;
				do {
					name = JOptionPane.showInputDialog(JMain.this,
							ex ? "Name already used, enter a new name" : "Enter a name", "New Project...",
							JOptionPane.INFORMATION_MESSAGE);
					if (name == null)
						return;
					ex = ProjectManager.checkForDuplicates(name);
				} while (ex);
				try {
					CardCreator.createNewProject(name);
					JMain.this.setTitle("Create Cards: " + CardCreator.getCurrentProjecttName());
					JMain.this.btnLoadFrontSide.setEnabled(true);
					JMain.this.btnLoadRearSide.setEnabled(true);
					JMain.this.btnLoadAlternateRear.setEnabled(true);
					populateRecentProjects();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		mnFile.add(mntmNewProject);

		JMenuItem mntmOpenProject = new JMenuItem("Open project");
		mntmOpenProject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String cmDir = System.getProperty("user.dir") + File.separator + "projects";
				try {
					if (!Files.exists(Paths.get(cmDir)))
						Files.createDirectories(Paths.get(cmDir));

					JFileChooser chooser = new JFileChooser(cmDir);
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					chooser.setAcceptAllFileFilterUsed(false);
					chooser.setDialogTitle("Choose a filename");
					chooser.setFileFilter(
							new FileNameExtensionFilter("CardMapperProject Files", new String[] { "cmp", "cmpz" }));
					int res = chooser.showOpenDialog(JMain.this);
					if (res == JFileChooser.APPROVE_OPTION) {
						Path selectedProject = chooser.getSelectedFile().toPath();
						String projectPath = selectedProject.toString();
						if (selectedProject.toAbsolutePath().toString().startsWith(CardCreator.getBaseFolder().toAbsolutePath().toString()))
							projectPath = CardCreator.getBaseFolder().relativize(selectedProject).toString();
						System.out.println("Open Project: " + projectPath);
						openProject(projectPath);
					}
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		});
		mnFile.add(mntmOpenProject);

		mntmSaveProject = new JMenuItem("Save project");
		mntmSaveProject.setEnabled(false);
		mntmSaveProject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				TaskScheduler.addTask("Saving project", 
					new Runnable() {
						public void run() {
							if (CardCreator.hasCurrentProject())
								CardCreator.setFileNameTemplate(JMain.this.tfFileNameTemplate.getText().trim());
							try {
								lblStatus.setText("Saving project file...");
								CardCreator.save();
								lblStatus.setText("Project file saved");
								populateRecentProjects();
							} catch (IOException e) {
								System.out.println("Error saving project file");
								lblStatus.setText("Error: Unable to save project file: " + e.getMessage());
								e.printStackTrace();
							}
						}
					}, 
					lblStatus,
					true
				);
			}
			
		});
		mnFile.add(mntmSaveProject);

		mnRecentProjects = new JMenu("Recent projects");
		mnFile.add(mnRecentProjects);
		
		JMenuItem mntmImportProject = new JMenuItem("Import project");
		mntmImportProject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String cmDir = System.getProperty("user.dir") + File.separator + "projects";
				try {
					if (!Files.exists(Paths.get(cmDir)))
						Files.createDirectories(Paths.get(cmDir));

					JFileChooser chooser = new JFileChooser(cmDir);
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					chooser.setAcceptAllFileFilterUsed(false);
					chooser.setDialogTitle("Choose a filename");
					chooser.setFileFilter(
							new FileNameExtensionFilter("CardMapperProject Files", new String[] { "cmp", "cmpz", "zip" }));
					int res = chooser.showOpenDialog(JMain.this);
					if (res == JFileChooser.APPROVE_OPTION) {
						TaskScheduler.addTask("Import project", new Runnable() {
							@Override
							public void run() {
								Path selectedProject = chooser.getSelectedFile().toPath();
								lblStatus.setText("Importing project \"" + (selectedProject != null ? selectedProject.getFileName().toString() : null) + "\"...");
								selectedProject = ProjectManager.importProject(selectedProject);
								if (selectedProject != null)
									try {
										JMain.this.openProject(selectedProject.toString());
										lblStatus.setText("Import of project \"" + (selectedProject != null ? selectedProject.getFileName().toString() : null) + "\" finished");
									} catch (IOException e) {
										e.printStackTrace();
										lblStatus.setText("Import of project \"" + (selectedProject != null ? selectedProject.getFileName().toString() : null) + "\" failed");
									}
								else
									lblStatus.setText("Import of project \"" + (selectedProject != null ? selectedProject.getFileName().toString() : null) + "\" failed");
							}
						}, lblStatus);
					}
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		});
		mnFile.add(mntmImportProject);
		
		mntmExportProject = new JMenuItem("Export Project");
		mntmExportProject.setEnabled(false);
		mntmExportProject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String cmDir = System.getProperty("user.dir") + File.separator + "projects";
				try {
					if (!Files.exists(Paths.get(cmDir)))
						Files.createDirectories(Paths.get(cmDir));
					JFileChooser chooser = new JFileChooserCheckExisting(cmDir);
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					chooser.setAcceptAllFileFilterUsed(false);
					chooser.setDialogTitle("Choose a filename");
					chooser.setFileFilter(
							new FileNameExtensionFilter("Compressed Project File (zip)", new String[] { "zip" }));
					int res = chooser.showSaveDialog(JMain.this);
					if (res == JFileChooser.APPROVE_OPTION) {
						Path exportFile = chooser.getSelectedFile().toPath();
						if (!exportFile.toString().toLowerCase().endsWith(".zip"))
							exportFile = Paths.get(exportFile.toString()+".zip");
						final Path exportFileFinal = exportFile;
						TaskScheduler.addTask("Project export",
							new Runnable() {
								public void run() {
									try {
									lblStatus.setText("Exporting project \"" + CardCreator.getCurrentProjecttName() + "\"");
									Path exP = CardCreator.exportProject(exportFileFinal);
									if (exP != null) {
										lblStatus.setText("Sucessfully exported project");
										Desktop.getDesktop().open(exportFileFinal.getParent().toFile());
									}
									else
										lblStatus.setText("An error occured while exporting the project");
									}
									catch(Exception e) {
										e.printStackTrace();
									}
								}
							},
							lblStatus
						);
					}
				}
				 catch (IOException e) {}
			}
		});
		mnFile.add(mntmExportProject);

		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int result = JOptionPane.showConfirmDialog(JMain.this, "Close program?", "Exiting...",
						JOptionPane.YES_NO_CANCEL_OPTION);
				switch (result) {
				case JOptionPane.YES_OPTION:
					System.exit(0);
					return;
				case JOptionPane.NO_OPTION:
					return;
				case JOptionPane.CLOSED_OPTION:
					return;
				case JOptionPane.CANCEL_OPTION:
					return;
				}
			}
		});
		mnFile.add(mntmExit);

		JMenu mnEdit = new JMenu("Edit");
		menuBar.add(mnEdit);

		mntmMapFields = new JMenuItem("Manage Fields");
		mntmMapFields.setEnabled(false);
		mntmMapFields.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFieldMappingPanel cm = new JFieldMappingPanel();
				boolean ok = JOptionPane.showConfirmDialog(JMain.this, cm, "Map Fields", JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
				if (ok) {
					List<Field> fields = cm.getFields();
					if (fields != null && CardCreator.hasCurrentProject()) {
						CardCreator.setFields(fields); 
					}
				}
			}
		});
		mnEdit.add(mntmMapFields);
		mnEdit.addSeparator();
		mntmImportCmFile = new JMenuItem("Import CM File");
		mntmImportCmFile.setEnabled(false);
		mntmImportCmFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String cmDir = System.getProperty("user.dir") + File.separator + "projects";
				try {
					if (!Files.exists(Paths.get(cmDir)))
						Files.createDirectories(Paths.get(cmDir));

					JFileChooser chooser = new JFileChooser(cmDir);
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					chooser.setAcceptAllFileFilterUsed(false);
					chooser.setDialogTitle("import CM File");
					chooser.setFileFilter(new FileNameExtensionFilter("CardMapper Files", new String[] { "cm" }));
					int res = chooser.showOpenDialog(JMain.this);
					if (res == JFileChooser.APPROVE_OPTION) {
						chooser.getSelectedFile();
						FieldPackage fp = FieldPackage.load(chooser.getSelectedFile().toPath());
						boolean overwrite = JOptionPane.showConfirmDialog(JMain.this,
								"OVerwrite existing fields and images?", "Import CM File",
								JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
						if (overwrite) {
							CardCreator.setFieldPackage(fp);
							loadFrontImage(fp.getFrontImage());
							loadRearImage(fp.getRearImage());
							revalidate();
						}
					}
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		});
		mnEdit.add(mntmImportCmFile);
		mnEdit.addSeparator();
		
		mntmConfiguration = new JMenuItem("Configuration");
		mntmConfiguration.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JConfigFrame config = new JConfigFrame(JMain.this);
				config.setVisible(true);
			}
		});
		mntmConfiguration.setEnabled(false);
		mnEdit.add(mntmConfiguration);

		mnFonts = new JMenu("Fonts");
		menuBar.add(mnFonts);

		mntmImportFont = new JMenuItem("Import Font");
		mntmImportFont.setEnabled(false);
		mntmImportFont.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String cmDir = System.getProperty("user.dir") + File.separator + "fonts";
				try {
					if (!Files.exists(Paths.get(cmDir)))
						Files.createDirectories(Paths.get(cmDir));

					JFileChooser chooser = new JFileChooser(cmDir);
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					chooser.setAcceptAllFileFilterUsed(false);
					chooser.setDialogTitle("Choose a filename");
					chooser.setFileFilter(new FileNameExtensionFilter("Font Files", new String[] { "ttf", "otf" }));
					int res = chooser.showOpenDialog(JMain.this);
					if (res == JFileChooser.APPROVE_OPTION) {
						Path selectedProject = chooser.getSelectedFile().toPath();
						String fontName = importFont(selectedProject.toString());
						lblStatus.setText("Successfully imported font " + fontName);
					}
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		});
		mnFonts.add(mntmImportFont);

		mnDeleteFont = new JMenu("Delete Font");
		mnFonts.add(mnDeleteFont);
		mnFonts.addSeparator();

		JMenu mnCsv = new JMenu("CSV");
		menuBar.add(mnCsv);

		mntmImportCsv = new JMenuItem("Import CSV, keep images external");
		mntmImportCsv.setEnabled(false);
		mntmImportCsv.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String cmDir = System.getProperty("user.dir") + File.separator + "csv";
				try {
					if (!Files.exists(Paths.get(cmDir)))
						Files.createDirectories(Paths.get(cmDir));

					JFileChooser chooser = new JFileChooser(cmDir);
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					chooser.setAcceptAllFileFilterUsed(false);
					chooser.setDialogTitle("Choose a filename");
					chooser.setFileFilter(new FileNameExtensionFilter("CSV file", new String[] { "csv" }));
					int res = chooser.showOpenDialog(JMain.this);
					if (res == JFileChooser.APPROVE_OPTION) {
						Path selectedCSV = chooser.getSelectedFile().toPath();
						CSVParser parser = ProjectManager.openCsv(selectedCSV, CardCreator.getCSVFormat());
						JCSVMapperPanel cm = new JCSVMapperPanel(CardCreator.getFields(), 
								new ArrayList<>(parser.getHeaderMap().keySet()), 
								CardCreator.getFieldMappings());
						boolean ok = JOptionPane.showConfirmDialog(JMain.this, cm, "Map CSV to Fields",
								JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
						parser.close();
						// Import csv as a task so it wont interrupt running card creation process
						Runnable payload = new Runnable() {
							@Override
							public void run() {
								try {
									CardCreator.importCsv(selectedCSV, false);
									lblStatus.setText("Successfully imported csv-file " + selectedCSV.getFileName().toString());
									if (ok) {
										Map<String, String> mapping = cm.getMappings();
										for (String field : mapping.keySet()) {
											CardCreator.addMapping(field, mapping.get(field));
										}
									}
									btnCreateCards.setEnabled(true);
									mntmMapCsv.setEnabled(true);
								} catch (IOException e) {
									lblStatus.setText("An Error occured while importing the csv-file " + selectedCSV.getFileName().toString());
									e.printStackTrace();
								}
							}
						};
						TaskScheduler.addTask("Import CSV data", payload, lblStatus);
						
					}
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		});
		mnCsv.add(mntmImportCsv);
		
		mntmImportCsv2 = new JMenuItem("Import CSV, integrate images");
		mntmImportCsv2.setEnabled(false);
		mntmImportCsv2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String cmDir = System.getProperty("user.dir") + File.separator + "csv";
				try {
					if (!Files.exists(Paths.get(cmDir)))
						Files.createDirectories(Paths.get(cmDir));

					JFileChooser chooser = new JFileChooser(cmDir);
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					chooser.setAcceptAllFileFilterUsed(false);
					chooser.setDialogTitle("Choose a filename");
					chooser.setFileFilter(new FileNameExtensionFilter("CSV file", new String[] { "csv" }));
					int res = chooser.showOpenDialog(JMain.this);
					if (res == JFileChooser.APPROVE_OPTION) {
						Path selectedCSV = chooser.getSelectedFile().toPath();
						CSVParser parser = ProjectManager.openCsv(selectedCSV, CardCreator.getCSVFormat());
						JCSVMapperPanel cm = new JCSVMapperPanel(CardCreator.getFields(), 
								new ArrayList<>(parser.getHeaderMap().keySet()), 
								CardCreator.getFieldMappings());
						boolean ok = JOptionPane.showConfirmDialog(JMain.this, cm, "Map CSV to Fields",
								JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
						parser.close();
						// Import csv as a task so it wont interrupt running card creation process
						Runnable payload = new Runnable() {
							@Override
							public void run() {
								try {
									CardCreator.importCsv(selectedCSV, true);
									lblStatus.setText("Successfully imported csv-file " + selectedCSV.getFileName().toString());
									if (ok) {
										Map<String, String> mapping = cm.getMappings();
										for (String field : mapping.keySet()) {
											CardCreator.addMapping(field, mapping.get(field));
										}
									}
									btnCreateCards.setEnabled(true);
									mntmMapCsv.setEnabled(true);
								} catch (IOException e) {
									lblStatus.setText("An Error occured while importing the csv-file " + selectedCSV.getFileName().toString());
									e.printStackTrace();
								}
							}
						};
						TaskScheduler.addTask("Import CSV data", payload, lblStatus);
						
					}
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		});
		mnCsv.add(mntmImportCsv2);

		mntmMapCsv = new JMenuItem("Map CSV to fields");
		mntmMapCsv.setEnabled(false);
		mntmMapCsv.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JCSVMapperPanel cm = new JCSVMapperPanel(CardCreator.getFields(), new ArrayList<>(CardCreator.getCsvHeader().keySet()), CardCreator.getFieldMappings());
				boolean ok = JOptionPane.showConfirmDialog(JMain.this, cm, "Map CSV to Fields",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
				// Import csv as a task so it wont interrupt running card creation process
				Runnable payload = new Runnable() {
					@Override
					public void run() {
						if (ok) {
							lblStatus.setText("Mapping CSV Columns to Fields");
							Map<String, String> mapping = cm.getMappings();
							for (String field : mapping.keySet()) {
								CardCreator.addMapping(field, mapping.get(field));
							}
							lblStatus.setText("Sucessfully mapped CSV Columns to Fields");
						}
					}
				};
				TaskScheduler.addTask("Import CSV data", payload, lblStatus);
			}
		});
		mnCsv.add(mntmMapCsv);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		JPanel panel = new JPanel();
		contentPane.add(panel, BorderLayout.CENTER);
		panel.setLayout(null);

		JLabel lblFrontSide = new JLabel("Front");
		lblFrontSide.setHorizontalAlignment(SwingConstants.CENTER);
		lblFrontSide.setBounds(10, 11, 133, 14);
		panel.add(lblFrontSide);
		frontImage = new BufferedImage(133, 199, BufferedImage.TYPE_INT_ARGB);
		pnFrontPreview = new JLabel(new ImageIcon(frontImage));
		pnFrontPreview.setToolTipText("Click to add fields");
		pnFrontPreview.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(CardCreator.hasCurrentProject()) {
					openFieldsEditor(true);
				}
			}
		});
		pnFrontPreview.setBorder(new LineBorder(new Color(0, 0, 0)));
		pnFrontPreview.setBounds(10, 36, 133, 199);
		panel.add(pnFrontPreview);

		btnLoadFrontSide = new JButton("Load front image");
		btnLoadFrontSide.setEnabled(false);
		btnLoadFrontSide.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				BufferedImage img = chooseImage();
				if (img != null) {
					boolean deleteFields = JOptionPane.showConfirmDialog(JMain.this,
							"Delete existing fields for the front side?", "Delete Fields",
							JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
					CardCreator.setFrontImage(img, deleteFields);
					loadFrontImage(img);
					System.out.println("Image Loaded");
				}
			}
		});
		btnLoadFrontSide.setBounds(10, 246, 133, 23);
		panel.add(btnLoadFrontSide);

		JLabel lblBackSide = new JLabel("Rear");
		lblBackSide.setHorizontalAlignment(SwingConstants.CENTER);
		lblBackSide.setBounds(163, 11, 133, 14);
		panel.add(lblBackSide);
		rearImage = new BufferedImage(133, 199, BufferedImage.TYPE_INT_ARGB);
		pnRearPreview = new JLabel(new ImageIcon(rearImage));
		pnRearPreview.setToolTipText("Click to add fields");
		pnRearPreview.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(CardCreator.hasCurrentProject()) {
					openFieldsEditor(false);
				}
			}
		});
		pnRearPreview.setBorder(new LineBorder(new Color(0, 0, 0)));
		pnRearPreview.setBounds(163, 36, 133, 199);
		panel.add(pnRearPreview);

		btnLoadRearSide = new JButton("Load rear image");
		btnLoadRearSide.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				BufferedImage img = chooseImage();
				if (img != null) {
					boolean delete = JOptionPane.showConfirmDialog(JMain.this,
							"Delete existing fields for the rear side?", "Delete Fields",
							JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
					CardCreator.setRearImage(img, delete);
					loadRearImage(img);
					System.out.println("Back Image Loaded");
				}
			}
		});
		btnLoadRearSide.setEnabled(false);
		btnLoadRearSide.setBounds(163, 246, 133, 23);
		panel.add(btnLoadRearSide);

		btnCreateCards = new JButton("Create Cards");
		btnCreateCards.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (CardCreator.hasCurrentProject()) {
					CardCreator.setFileNameTemplate(tfFileNameTemplate.getText());
					TaskScheduler.addTask("Create cards", new Runnable() {
						@Override
						public void run() {
							try {
								CardCreator.drawCards(lblStatus);
								lblStatus.setText("Drawing cards finished");
							} catch (IOException e1) {
								lblStatus.setText("Error drawing cards: " + e1.getMessage());
								e1.printStackTrace();
							}
						}
					}, lblStatus);
				}
			}
		});
		btnCreateCards.setToolTipText("Import CSV first");
		btnCreateCards.setEnabled(false);
		btnCreateCards.setBounds(10, 280, 440, 23);
		panel.add(btnCreateCards);

		tfFileNameTemplate = new JTextField();
		tfFileNameTemplate.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				CardCreator.setFileNameTemplate(tfFileNameTemplate.getText());
			}
		});
		tfFileNameTemplate.setEnabled(false);
		tfFileNameTemplate.setBounds(10, 328, 405, 20);
		panel.add(tfFileNameTemplate);
		tfFileNameTemplate.setColumns(10);

		btnI = new JButton("i");
		btnI.setEnabled(false);
		btnI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				List<String> fileOptions = new LinkedList<>();
				fileOptions.add("Internal Variables");
				fileOptions.add(" {no} - card number");
				fileOptions.add(" {side} - card side (front|back)");
				if (CardCreator.getCsvHeader() != null) {
					fileOptions.add("Fields from the CSV:");
					fileOptions.addAll(CardCreator.getCsvHeader().keySet().stream()
							.map(s -> " {" + s + "}").collect(Collectors.toList()));
				}
				JPanel info = new JPanel();
				info.setLayout(null);
				int c = 0;
				for (String option : fileOptions) {
					JLabel l = new JLabel(option);
					l.setBounds(0, c++ * 24, 200, 20);
					info.add(l);
				}
				info.setBounds(0, 0, 200, c * 24);
				info.setSize(new Dimension(200, c * 24));
				info.setPreferredSize(new Dimension(200, c * 24));
				info.setMinimumSize(new Dimension(200, c * 24));
				info.setMaximumSize(new Dimension(200, c * 24));
				// JOptionPane pane = new JOptionPane("Placeholder for card
				// filenames", JOptionPane.INFORMATION_MESSAGE);
				JDialog dia = new JDialog(JMain.this, "Placeholder for card filenames", false);
				dia.getContentPane().add(info, BorderLayout.CENTER);
				dia.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				Point p = JMain.this.getLocation();
				p = new Point((int) p.getX() + 250, (int) p.getY() + 100);
				dia.setLocation(p);
				dia.pack();
				dia.setVisible(true);
				// JOptionPane.showConfirmDialog(Main.this,
				// info,
				// "Placeholder for card filenames",
				// JOptionPane.OK_OPTION,
				// JOptionPane.PLAIN_MESSAGE);
			}
		});
		btnI.setBounds(425, 327, 25, 23);
		panel.add(btnI);

		JLabel lblFilenameTemplate = new JLabel("Filename template");
		lblFilenameTemplate.setBounds(10, 314, 133, 14);
		panel.add(lblFilenameTemplate);

		alternateRearImage = new BufferedImage(133, 199, BufferedImage.TYPE_INT_ARGB);
		pnAlternateRearPreview = new JLabel(new ImageIcon(alternateRearImage));
		pnAlternateRearPreview.setToolTipText("Click to add fields");
		pnAlternateRearPreview.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(CardCreator.hasCurrentProject()) {
					openFieldsEditor(false);
				}
			}
		});
		pnAlternateRearPreview.setBorder(new LineBorder(new Color(0, 0, 0)));
		pnAlternateRearPreview.setBounds(316, 36, 133, 199);
		panel.add(pnAlternateRearPreview);
		
		JLabel lblAlternateBackSide = new JLabel("Alternate rear");
		lblAlternateBackSide.setHorizontalAlignment(SwingConstants.CENTER);
		lblAlternateBackSide.setBounds(317, 11, 133, 14);
		panel.add(lblAlternateBackSide);
		
		btnLoadAlternateRear = new JButton("Load alternate rear");
		btnLoadAlternateRear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				BufferedImage img = chooseImage();
				if (img != null) {
					CardCreator.setAlternateRearImage(img);
					loadAlternateRearImage(img);
					System.out.println("Alternate rear image loaded");
				}
			}
		});
		btnLoadAlternateRear.setEnabled(false);
		btnLoadAlternateRear.setBounds(316, 246, 133, 23);
		panel.add(btnLoadAlternateRear);

		pnStatus = new JPanel();
		pnStatus.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		pnStatus.setPreferredSize(new Dimension(this.getWidth(), 20));
		contentPane.add(pnStatus, BorderLayout.SOUTH);
		pnStatus.setLayout(new BoxLayout(pnStatus, BoxLayout.X_AXIS));

		lblStatus = new JStatusLabel("Status");
		lblStatus.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				JLogPanel jlog = new JLogPanel(lblStatus);
				JDialog dia = new JDialog(JMain.this, "Log", false);
				dia.getContentPane().add(jlog, BorderLayout.CENTER);
				dia.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				Point p = JMain.this.getLocation();
				p = new Point((int) p.getX() + 250, (int) p.getY() + 100);
				dia.setLocation(p);
				dia.pack();
				dia.setVisible(true);
			}
		});
		lblStatus.setHorizontalAlignment(SwingConstants.LEFT);
		pnStatus.add(lblStatus);

		Component verticalStrut = Box.createVerticalStrut(20);
		pnStatus.add(verticalStrut);

		Component horizontalGlue = Box.createHorizontalGlue();
		pnStatus.add(horizontalGlue);

		JButton btnTasks = new JButton("Tasks");
		btnTasks.setMargin(new Insets(2, 2, 2, 2));
		btnTasks.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showTaskScheduler();
			}
		});
		btnTasks.setPreferredSize(new Dimension(60, 20));
		btnTasks.setSize(new Dimension(60, 20));
		btnTasks.setMaximumSize(new Dimension(60, 20));
		pnStatus.add(btnTasks);
		populateRecentProjects();
	}

	private void loadFrontImage(BufferedImage img) {
		if (img != null) {
			Image smal = img.getScaledInstance(133, 199, BufferedImage.SCALE_SMOOTH);
			Graphics2D g = frontImage.createGraphics();
			g.drawImage(smal, 0, 0, null);
			g.dispose();
			pnFrontPreview.repaint();
		} else {
			Graphics2D g = frontImage.createGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, frontImage.getWidth(), frontImage.getHeight());
			g.dispose();
			pnFrontPreview.repaint();
		}
	}

	private void loadRearImage(BufferedImage img) {
		if (img != null) {
			Image smal = img.getScaledInstance(133, 199, BufferedImage.SCALE_SMOOTH);
			Graphics2D g = rearImage.createGraphics();
			g.drawImage(smal, 0, 0, null);
			g.dispose();
			pnRearPreview.repaint();
		} else {
			Graphics2D g = rearImage.createGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, rearImage.getWidth(), rearImage.getHeight());
			g.dispose();
			pnRearPreview.repaint();
		}
	}

	private void loadAlternateRearImage(BufferedImage img) {
		if (img != null) {
			Image smal = img.getScaledInstance(133, 199, BufferedImage.SCALE_SMOOTH);
			Graphics2D g = alternateRearImage.createGraphics();
			g.drawImage(smal, 0, 0, null);
			g.dispose();
			pnAlternateRearPreview.repaint();
		} else {
			Graphics2D g = alternateRearImage.createGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, rearImage.getWidth(), alternateRearImage.getHeight());
			g.dispose();
			pnAlternateRearPreview.repaint();
		}
	}

	private BufferedImage chooseImage() {
		JFileChooser chooser = new JFileChooser(
				System.getProperty("user.home") + System.getProperty("file.separator") + "Pictures");
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.setDialogTitle("Select the front image, all rectangles will be discarded");
		chooser.setFileFilter(new FileNameExtensionFilter("Image files", ImageIO.getReaderFileSuffixes()));
		int res = chooser.showOpenDialog(null);
		if (res == JFileChooser.APPROVE_OPTION) {
			System.out.println("Load Image...");
			String path = chooser.getSelectedFile().toString();
			try {
				return ImageIO.read(new File(path));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private void populateRecentProjects() {
		// mnRecentProjects;
		List<String> recent = new ArrayList<>(ProjectManager.getRecentProjects());
		Collections.reverse(recent);
		mnRecentProjects.removeAll();
		for (String pro : recent) {
			JMenuItem jmit = new JMenuItem(pro);
			jmit.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					try {
						openProject(((JMenuItem) arg0.getSource()).getText());
					} catch (IOException e) {
						System.out.println("Error opening project");
						lblStatus.setText("Error: Project could not be opened");
						e.printStackTrace();
					}
				}
			});
			mnRecentProjects.add(jmit);
		}
		mnRecentProjects.revalidate();
	}

	private void openProject(String selectedProject) throws IOException {
		CardCreator.openProject(selectedProject);
		JMain.this.setTitle("Create Cards: " + CardCreator.getProjectName());
		JMain.this.loadFrontImage(CardCreator.getFrontImage());
		JMain.this.loadRearImage(CardCreator.getRearImage());
		JMain.this.loadAlternateRearImage(CardCreator.getAlternateRearImage());
		JMain.this.tfFileNameTemplate.setText(CardCreator.getFileNameTemplate());
		JMain.this.btnLoadFrontSide.setEnabled(true);
		JMain.this.mntmSaveProject.setEnabled(true);
		JMain.this.btnLoadRearSide.setEnabled(true);
		JMain.this.btnLoadAlternateRear.setEnabled(true);
		JMain.this.mntmImportCmFile.setEnabled(true);
		JMain.this.mntmImportCsv.setEnabled(true);
		JMain.this.mntmImportCsv2.setEnabled(true);
		JMain.this.mntmMapFields.setEnabled(true);
		JMain.this.mntmImportFont.setEnabled(true);
		JMain.this.mntmExportProject.setEnabled(true);
		JMain.this.mntmConfiguration.setEnabled(true);
		JMain.this.tfFileNameTemplate.setEnabled(true);
		JMain.this.btnI.setEnabled(true);
		if (CardCreator.getCsvHeader() != null) {
			JMain.this.btnCreateCards.setEnabled(true);
			JMain.this.mntmMapCsv.setEnabled(true);
		}
		populateDeleteFontMenu();
		lblStatus.setText("Project opened");
		revalidate();
		populateRecentProjects();
	}

	private String importFont(String selectedFont) throws IOException, FontFormatException {
		String fontName = CardCreator.addFont(Paths.get(selectedFont));
		populateDeleteFontMenu(fontName);
		return fontName;
	}

	private void populateDeleteFontMenu() {
		mnDeleteFont.removeAll();
		if (CardCreator.hasCurrentProject()) {
			Map<String, Font> fonts = CardCreator.getProjectFonts();
			for (String fontName : fonts.keySet()) {
				populateDeleteFontMenu(fontName);
			}
		}
	}

	/**
	 * Adds a menu entry in the delete font submenu for the given font name
	 * 
	 * @param fontName
	 *            Name of the font that can be deleted with the added entry
	 */
	private void populateDeleteFontMenu(String fontName) {
		JMenuItem dFont = new JMenuItem(fontName);
		dFont.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int answer = JOptionPane.showConfirmDialog(JMain.this,
						"Delete font " + ((JMenuItem) e.getSource()).getText() + "?", "Delete Font",
						JOptionPane.YES_NO_OPTION);
				if (answer == JOptionPane.YES_OPTION) {
					Runnable payload = new Runnable() {
						@Override
						public void run() {
							try {
								CardCreator.removeFont(((JMenuItem) e.getSource()).getText());
								lblStatus.setText("Font " + ((JMenuItem) e.getSource()).getText() + " successfully deleted");
								mnDeleteFont.remove(((JMenuItem) e.getSource()));
							} catch (IOException e1) {
								lblStatus.setText("Error deleting font " + ((JMenuItem) e.getSource()).getText() + ": " + e1.getMessage());
								e1.printStackTrace();
							} 
						}
					};
					TaskScheduler.addTask("Delete Font " + ((JMenuItem) e.getSource()).getText(), payload, lblStatus);
				}
			}
		});
		mnDeleteFont.add(dFont);
		revalidate();
	}

	private void showTaskScheduler() {
		JTaskScheduler scheduler = new JTaskScheduler();
		JDialog dia = new JDialog(JMain.this, "Task Scheduler", false);
		dia.getContentPane().add(scheduler, BorderLayout.CENTER);
		dia.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		Point p = JMain.this.getLocation();
		p = new Point((int) p.getX() + 250, (int) p.getY() + 100);
		dia.setLocation(p);
		dia.pack();
		dia.addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {}
			
			@Override
			public void windowIconified(WindowEvent e) {}
			
			@Override
			public void windowDeiconified(WindowEvent e) {}
			
			@Override
			public void windowDeactivated(WindowEvent e) {}
			
			@Override
			public void windowClosing(WindowEvent e) {}
			
			@Override
			public void windowClosed(WindowEvent e) {
				scheduler.stopDaemon();
			}
			
			@Override
			public void windowActivated(WindowEvent e) {}
		});
		dia.setVisible(true);
	}

	private void openFieldsEditor(boolean frontPanel) {
		JFieldMappingPanel cm = new JFieldMappingPanel(frontPanel);
		boolean ok = JOptionPane.showConfirmDialog(JMain.this, cm, "Map Fields", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
		if (ok) {
			List<Field> fields = cm.getFields();
			if (fields != null && CardCreator.hasCurrentProject()) {
				CardCreator.setFields(fields); 
			}
		}
	}
}
