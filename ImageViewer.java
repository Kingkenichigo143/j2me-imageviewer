import java.io.*;
import java.util.Enumeration;
import java.util.Stack;

import javax.microedition.io.*;
import javax.microedition.io.file.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import javax.microedition.rms.*;
import javax.microedition.sensor.*;

// autorotate enabled only if jad contains "auto-rotate: true"

class AccelSensor implements Runnable {
    protected SensorConnection sconn;
    public boolean hasSensor;
    public Thread thr;
    protected boolean stop_thread;
    ImageViewer parent;


    AccelSensor(ImageViewer parent) {
	this.parent = parent;
        String sprop = System.getProperty("microedition.sensor.version");

        // autorotate enabled only if jad contains "auto-rotate: true"
        String propAutoRotate = parent.getAppProperty("auto-rotate"); 

        if (sprop == null
            || propAutoRotate == null 
            || !propAutoRotate.toLowerCase().equals("true")) 
        {
            hasSensor = false;
            return;
        }

        try {
            sconn = (SensorConnection) Connector.open("sensor:acceleration");
            hasSensor = true;
        } catch (Exception e) {
            hasSensor = false;
        }
    }

    
    public void stopthread() {
	stop_thread = true;
    }    
    
    boolean hasSensor() {
	return hasSensor;	
    }

    public void start() {
	if(!hasSensor()) return;
	stop_thread = false;
	thr = new Thread(this);
	thr.start();
    }
    
    public synchronized void run() {
        while(!stop_thread) {
             try {
             wait(1000); 
             sensorData();
             orient();
             } catch (Exception e) {
             System.out.println(e);
             e.printStackTrace();
             }
    //	     System.out.println("sensor..." + sensorcnt++);
        }
    }
    
    
    
    private int sensorcnt;
    public double sensorReading;
    public double sensorReading2;
    
    private void sensorData() throws IOException {
	Data[] data = sconn.getData(1);
	//ChannelInfo cInfo = data[0].getChannelInfo();
	//int dataType = cInfo.getDataType();	
	//dc assuming double and no scale
	double[] values = data[0].getDoubleValues();
	sensorReading = (values.length > 0) ? values[0] : 0.0;

	values = data[1].getDoubleValues();
	sensorReading2 = (values.length > 0) ? values[0] : 0.0;
	System.out.println("sensorReading[0,1] = " + sensorReading + " " + sensorReading2);
    }

    // Orientation
    public static final int LEFT = 1;
    public static final int UP = 2;
    public static final int RIGHT = 3;
    public static final int UPSIDEDOWN = 4;
   
    private void orient() {
        // -4 ... 4 = UP   
        // <-7 = turned RIGHT
        // >7 = turned LEFT
        int currOrient = orientation;
        if(sensorReading < -7) {
            currOrient = RIGHT;
        } else if(-4 < sensorReading  && sensorReading < 4) {
            currOrient = UP;
        } else if(sensorReading > 7) {
            currOrient = LEFT;
        } 

	// sensorReading2 < 0 = tilted near flat, sensorReading rotate reduced
        if(sensorReading2 < 0) {
	    if(sensorReading < -4) {
                currOrient = RIGHT;
            } else if(4 < sensorReading) {
                currOrient = LEFT;
            }
        } 

        if(currOrient != orientation) {
            orientChanged(currOrient);
        }
    }
    
    private void orientChanged(int newOrient) {
        switch (orientation) {
            case UP:
                if (newOrient == LEFT) {
                    parent.commandAction(parent.rotate, null);
                } else if (newOrient == RIGHT) {
                    parent.commandAction(parent.rotateccw, null);
                }
                break;
            case LEFT:
                if (newOrient == UP) {
                    parent.commandAction(parent.rotateccw, null);
                } else if (newOrient == RIGHT) {
                    parent.commandAction(parent.rotate, null);
                    parent.commandAction(parent.rotate, null);
                }
                break;
            case RIGHT:
                if (newOrient == UP) {
                    parent.commandAction(parent.rotate, null);
                } else if (newOrient == LEFT) {
                    parent.commandAction(parent.rotate, null);
                    parent.commandAction(parent.rotate, null);
                }
                break;

        }
	    
        orientation = newOrient;    
    }
    
    public int orientation = UP;
}


public class ImageViewer extends MIDlet implements CommandListener {
    final static String         HORZ_SEP     = "------------------------";
    private final static String MEGA_ROOT    = "/";
    private final static char   SEP          = '/';
    private final static String SEP_STR      = "/";
    private final static String UP_DIRECTORY = "[UP DIR]";

    /* screen commands */
    private Command view = new Command("View", Command.ITEM, 1);

    /* image commands */
    Command              rotateccw = new Command("RotateCCW", Command.SCREEN, 1); // only used by sensor

    Command              rotate    = new Command("Rotate", Command.SCREEN, 1);
    Command              exit      = new Command("Exit", Command.EXIT, 3);
    Command              back      = new Command("Back", Command.BACK, 2);
    Command              zoomin    = new Command("zoom +", Command.SCREEN, 1);
    Command              zoomout   = new Command("zoom -", Command.SCREEN, 1);
    Command              imginfo   = new Command("info", Command.SCREEN, 2);
    
    MRU                  mru       = new MRU();
    List                 browser;
    private String       currDirName;
    private ScrollCanvas image;
    private Form         imgInfoForm;
    boolean              rotatef;
    
    AccelSensor          accelSensor;

    public ImageViewer() {
        currDirName = MEGA_ROOT;
    }

    public void startApp() {
        loadRMS();

        boolean isAPIAvailable = false;

        if (System.getProperty("microedition.io.file.FileConnection.version") != null) {
            isAPIAvailable = true;

            try {
                showCurrDir();
            } catch (SecurityException e) {
                System.out.println(e);
            } catch (Exception e) {
                System.out.println(e);
            }
        } else {
            StringBuffer splashText = new StringBuffer(getAppProperty("MIDlet-Name")).append("\n").append(
                                          getAppProperty("MIDlet-Vendor")).append(isAPIAvailable
                    ? ""
                    : "\nFileConnection API not available");
            Alert splashScreen = new Alert(null, splashText.toString(), null, AlertType.INFO);

            splashScreen.setTimeout(3000);
            Display.getDisplay(this).setCurrent(splashScreen);
        }
	
	accelSensor = new AccelSensor(this);
	accelSensor.start();
    }

    public void pauseApp() {}

    public void destroyApp(boolean cond) {
        storeRMS();
        notifyDestroyed();
    }

    public void loadRMS() {
        RecordStore recordStore = null;

        try {
            recordStore = RecordStore.openRecordStore("mru", true);    // 0 = curr path, 1-3 = files

            RecordEnumeration e = recordStore.enumerateRecords(null, null, false);

            if (!e.hasNextElement()) {                                 // init record
                return;
            }

            int             recId = e.nextRecordId();
            byte[]          bMRU  = recordStore.getRecord(recId);
            DataInputStream dis   = new DataInputStream(new ByteArrayInputStream(bMRU));

            mru.path = dis.readUTF();

            while (dis.available() != 0) {
                mru.files.push(dis.readUTF());
            }

            // delete all records
            e.reset();

            while (e.hasNextElement()) {
                recordStore.deleteRecord(e.nextRecordId());
            }
        } catch (RecordStoreException rse) {
            System.out.println(rse);
            rse.printStackTrace();
        } catch (IOException ioe) {
            System.out.println(ioe);
            ioe.printStackTrace();
        } finally {
            try {
                recordStore.closeRecordStore();
            } catch (Exception e) {}
        }
    }

    public void storeRMS() {
        Stack files = new Stack();    // used to reverse order so oldest on top

        try {
            RecordStore           recordStore = RecordStore.openRecordStore("mru", true);    // 0 = curr path, 1-3 = files
            ByteArrayOutputStream bos         = new ByteArrayOutputStream();
            DataOutputStream      dos         = new DataOutputStream(bos);

            if (mru.path != null) {
                dos.writeUTF(mru.path);
            } else {
                dos.writeUTF("");
            }

            try {
                for (int i = 3; (i > 0) &&!mru.files.empty(); i--) {
                    files.push(mru.files.pop());
                }
            } catch (Exception e) {}

            while (!files.empty()) {
                dos.writeUTF((String) files.pop());
            }

            dos.flush();
            recordStore.addRecord(bos.toByteArray(), 0, bos.size());
        } catch (RecordStoreException rse) {
            System.out.println(rse);
            rse.printStackTrace();
        } catch (IOException ioe) {
            System.out.println(ioe);
            ioe.printStackTrace();
        }
    }

    public void commandAction(Command c, Displayable d) {

        System.out.println("command:" + c + " displayable:" + d);
        
        if (c == view) {
            List         curr     = (List) d;
            final String currFile = curr.getString(curr.getSelectedIndex());

            System.out.println("currFile:" + currFile);
            new Thread(new Runnable() {
                public void run() {
                    if (currFile.endsWith(SEP_STR) || currFile.equals(UP_DIRECTORY)) {
                        System.out.println("traverseDir: " + currFile);
                        traverseDirectory(currFile);
                    } else {
                        System.out.println("showFile: " + currDirName + currFile);
                        showFile(currDirName + currFile);
                    }
                }
            }).start();
        } else if (c == back) {
            if (d == image) {
// Put in separate thread to avoid in emulator:
// java.lang.RuntimeException: Blocking call performed in the event thread 
                
                // reclaim memory
                Display.getDisplay(this).setCurrent(null);
                image.im = null;
                image.imoriginal = null;
                image = null;
                System.gc();
                
                new Thread(new Runnable() {

                    public void run() {
                        System.out.println("scd thread");
                        showCurrDir();
                    }
                }).start();
            } else if (d == imgInfoForm) {
                Display.getDisplay(this).setCurrent(image);
            }
        } else if (c == rotate) {
            image.rotateImage(ScrollCanvas.ROTATE_CW);
        // used by sensor
        } else if (c == rotateccw) {
            image.rotateImage(ScrollCanvas.ROTATE_CCW);
        } else if (c == zoomin) {
            image.zoomin();

        } else if (c == zoomout) {
            image.zoomout();
             
        } else if (c == imginfo) {
            // maybe move this code to a separate method or class
            imgInfoForm = new Form("Image Info");
            imgInfoForm.append("Width: " + image.im.getWidth());
            imgInfoForm.append("Height: " + image.im.getHeight());
            imgInfoForm.append("Name: " + image.fconn.getName());
            imgInfoForm.append("Path: " + image.fconn.getPath());
            try {
            imgInfoForm.append("Size: " + image.fconn.fileSize()); 
            } catch (Exception e) {};
            imgInfoForm.append("Free Mem: " + Runtime.getRuntime().freeMemory() + "/" + Runtime.getRuntime().totalMemory());
                      
            imgInfoForm.addCommand(back);
            imgInfoForm.setCommandListener(this);
            Display.getDisplay(this).setCurrent(imgInfoForm);
            
        } else if (c == exit) {
            destroyApp(false);
        }
    }

    void showCurrDir() {
        Enumeration    e;
        FileConnection currDir = null;

        try {
            System.out.println("In showCurrDir");
            System.out.println("mega_root:" + MEGA_ROOT + " cur_dir:" + currDirName);

            if (MEGA_ROOT.equals(currDirName)) {
                browser = new List(currDirName, List.IMPLICIT);
                mru.addToList(browser);
                e = FileSystemRegistry.listRoots();
            } else {
                System.out.println("connector: " + currDirName);
                currDir = (FileConnection) Connector.open("file://localhost/" + currDirName, Connector.READ);

                e       = currDir.list();
                browser = new List(currDirName, List.IMPLICIT);
                browser.append(UP_DIRECTORY, null);
            }

            while (e.hasMoreElements()) {

                String fileName = (String) e.nextElement();

                System.out.println("fileName:" + fileName + " char_at:" + fileName.charAt(fileName.length() - 1));

                browser.append(fileName, null);
            }

            browser.setSelectCommand(view);
            browser.addCommand(exit);
            browser.setCommandListener(this);
            Display.getDisplay(this).setCurrent(browser);

            if (currDir != null) {
                currDir.close();
            }
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }

    void traverseDirectory(String fileName) {
        System.out.println("fileName:" + fileName + " cur_dir:" + currDirName + " mega_root:" + MEGA_ROOT);

        if (fileName.equals(HORZ_SEP)) {
            return;    // separater
        }

        if (currDirName.equals(MEGA_ROOT)) {
            if (fileName.equals(UP_DIRECTORY)) {

                // can not go up from MEGA_ROOT
                return;
            }

            currDirName = fileName;
        } else if (fileName.equals(UP_DIRECTORY)) {
            System.out.println("up");

            // Go up one directory
            int i = currDirName.lastIndexOf(SEP, currDirName.length() - 2);

            if (i != -1) {
                currDirName = currDirName.substring(0, i + 1);
            } else {
                currDirName = MEGA_ROOT;
            }
        } else {
            currDirName = currDirName + fileName;
        }

        if (!currDirName.equals(MEGA_ROOT)) {
            mru.path = currDirName;
        }

        showCurrDir();
    }

    private int showFileRetry = 0; // showFile retry state by retry using callSerially to give KVM chance to free memroy
    void showFile(String fileName) {
        final String ffileName = fileName;
        if (fileName.startsWith("/")) {
            fileName = fileName.substring(1);
        }
        System.out.println("free:" + Runtime.getRuntime().freeMemory());
        System.out.println("image: " + image);


        try {
            image = new ScrollCanvas(this, "file:///" + fileName);
        } catch (OutOfMemoryError e) {
            System.gc();
            
            // stop retrying showFile() at # tries
            if(showFileRetry == 2) {
                alert("Out of memory! [sf]");
                showFileRetry = 0;
                return;
            }
            
            showFileRetry += 1;
            Runnable runnable = new Runnable() {
                public void run() {
                    System.out.println("serial rerun showFile(" + ffileName + " #" + showFileRetry);
                    showFile(ffileName);
                }
            };
            Display.getDisplay(this).callSerially(runnable);
            return;
        }
        mru.files.removeElement(fileName);
        mru.files.push(fileName);
        browser.removeCommand(exit);
        image.addCommand(zoomin);
        image.addCommand(zoomout);
        image.addCommand(rotate);
        image.addCommand(imginfo);
        image.addCommand(back);
        image.setCommandListener(this);
        Display.getDisplay(this).setCurrent(image);
    }
    
    private Alert alert;
    void alert(String msg) {
        if(alert == null) {
            alert = new Alert("Alert");
//            alert.setTimeout(Alert.FOREVER);
            alert.setTimeout(1000);
            alert.setType(AlertType.ERROR);
        }
        
        alert.setString(msg);
        System.out.println("alert " + msg);
        Display d = Display.getDisplay(this);

        d.vibrate(100);
        d.setCurrent(alert);
    }
}

// Most Recently Used data struct.  Stores 1 path entry, up to 3 file entries.  
class MRU {
    Stack  files;
    String path;

    MRU() {
        path  = null;
        files = new Stack();
    }

    // add MRU entries to file browser (List) screen
    void addToList(List browser) {
        String s;

        if ((path != null) && !path.equals("")) {
            browser.append(path, null);
        }

        if (files.empty()) {
            if (path != null) {
                browser.append(ImageViewer.HORZ_SEP, null);
            }

            return;
        }

        for (int i = 1; i <= 3; i++) {
            if (i > files.size()) {
                break;
            }

            s = (String) files.elementAt(files.size() - i);

            if (!s.equals("")) {
                browser.append(s, null);
            }
        }

        browser.append(ImageViewer.HORZ_SEP, null);
    }
}
