import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.*;
import java.util.Vector;
import java.io.*;
import javax.microedition.io.*;

public class KMRadio extends MIDlet implements CommandListener {
    private Display display;
    private List mainList;
    private Command exitCommand, addCommand, deleteCommand, playCommand, backCommand, saveCommand, loadCommand;
    private Form addForm;
    private TextField urlField, nameField;
    private Vector favorites;
    private Form splashScreen;
    private boolean firstStart = true;
    private static final String STATION_URL = "http://your-server-address/stationlist.txt"; 

    public KMRadio() {
        display = Display.getDisplay(this);
        mainList = new List("KM Radio", List.IMPLICIT);
        favorites = new Vector();
        loadFavorites();

        splashScreen = new Form("KM Radio");
        splashScreen.append(new StringItem(null, "Welcome to KM Radio\n\nLoading..."));
    }

    protected void startApp() {
        if (firstStart) {
            display.setCurrent(splashScreen);
            firstStart = false;
            new Thread() {
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {}
                    initializeUI();
                }
            }.start();
        } else {
            display.setCurrent(mainList);
        }
    }

    private void initializeUI() {
        exitCommand = new Command("Exit", Command.EXIT, 1);
        addCommand = new Command("Add", Command.SCREEN, 1);
        deleteCommand = new Command("Delete", Command.ITEM, 2);
        playCommand = new Command("Play", Command.ITEM, 1);
        backCommand = new Command("Back", Command.BACK, 1);
        saveCommand = new Command("Save", Command.OK, 1);
        loadCommand = new Command("Load from Server", Command.SCREEN, 2);

        mainList.addCommand(exitCommand);
        mainList.addCommand(addCommand);
        mainList.addCommand(deleteCommand);
        mainList.addCommand(playCommand);
        mainList.addCommand(loadCommand);
        mainList.setCommandListener(this);

        addForm = new Form("Add Station");
        nameField = new TextField("Name:", "", 32, TextField.ANY);
        urlField = new TextField("URL:", "", 256, TextField.URL);
        addForm.append(nameField);
        addForm.append(urlField);
        addForm.addCommand(backCommand);
        addForm.addCommand(saveCommand);
        addForm.setCommandListener(this);

        updateList();
        display.setCurrent(mainList);
    }

    private void updateList() {
        mainList.deleteAll();
        for (int i = 0; i < favorites.size(); i++) {
            String[] station = (String[]) favorites.elementAt(i);
            mainList.append(station[0], null);
        }
    }
	
	private void deleteStation(int index) {
    if (index >= 0 && index < favorites.size()) {
        favorites.removeElementAt(index);
        saveFavorites();  // Save changes after deletion
        updateList();     // Refresh the UI list
    }
}

	

    private void loadFavorites() {
        try {
            RecordStore rs = RecordStore.openRecordStore("favorites", true);
            if (rs.getNumRecords() > 0) {
                byte[] raw = rs.getRecord(1);
                String data = new String(raw);
                String[] stations = split(data, ";");
                for (int i = 0; i < stations.length; i++) {
                    String[] station = split(stations[i], ",");
                    if (station.length == 2) {
                        favorites.addElement(station);
                    }
                }
            }
            rs.closeRecordStore();
        } catch (Exception e) {
            display.setCurrent(new Alert("Notice", "Welcome to KM Radio", null, AlertType.INFO));
        }
    }

    private void saveFavorites() {
        try { RecordStore.deleteRecordStore("favorites"); } catch (Exception ignored) {}
        try {
            RecordStore rs = RecordStore.openRecordStore("favorites", true);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < favorites.size(); i++) {
                String[] station = (String[]) favorites.elementAt(i);
                sb.append(station[0]).append(",").append(station[1]);
                if (i < favorites.size() - 1) sb.append(";");
            }
            rs.addRecord(sb.toString().getBytes(), 0, sb.length());
            rs.closeRecordStore();
        } catch (Exception e) {
            display.setCurrent(new Alert("Error", "Could not save stations", null, AlertType.ERROR));
        }
    }

    private String[] split(String str, String delimiter) {
        Vector parts = new Vector();
        int index = 0, lastIndex = 0;
        while ((index = str.indexOf(delimiter, lastIndex)) != -1) {
            parts.addElement(str.substring(lastIndex, index));
            lastIndex = index + delimiter.length();
        }
        parts.addElement(str.substring(lastIndex));
        String[] result = new String[parts.size()];
        parts.copyInto(result);
        return result;
    }

    private void loadStationsFromServer() {
        new Thread() {
            public void run() {
                try {
                    HttpConnection conn = (HttpConnection) Connector.open(STATION_URL);
                    InputStream is = conn.openInputStream();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    int ch;
                    while ((ch = is.read()) != -1) {
                        bos.write(ch);
                    }
                    is.close();
                    conn.close();
                    String data = new String(bos.toByteArray());
                    String[] stations = split(data, "\n");
                    favorites.removeAllElements();
                    for (int i = 0; i < stations.length; i++) {
                        String[] station = split(stations[i], ",");
                        if (station.length == 2) {
                            favorites.addElement(station);
                        }
                    }
                    updateList();
                } catch (Exception e) {
                    display.setCurrent(new Alert("Error", "Could not load stations", null, AlertType.ERROR));
                }
            }
        }.start();
    }

    public void commandAction(Command c, Displayable d) {
        if (c == exitCommand) {
            destroyApp(false);
            notifyDestroyed();
        } else if (c == addCommand) {
            display.setCurrent(addForm);
        } else if (c == backCommand) {
            display.setCurrent(mainList);
        } else if (c == deleteCommand) {
            int idx = mainList.getSelectedIndex();
            if (idx >= 0) deleteStation(idx);
        } else if (c == playCommand) {
            int idx = mainList.getSelectedIndex();
            if (idx >= 0) {
                try {
                    platformRequest(((String[]) favorites.elementAt(idx))[1]);
                } catch (Exception e) {
                    display.setCurrent(new Alert("Error", "Could not play station", null, AlertType.ERROR));
                }
            }
        } else if (c == saveCommand) {
            String name = nameField.getString().trim();
            String url = urlField.getString().trim();
            if (name.length() > 0 && url.length() > 0) {
                favorites.addElement(new String[]{name, url});
                saveFavorites();
                updateList();
                display.setCurrent(mainList);
            }
        } else if (c == loadCommand) {
            loadStationsFromServer();
        }
    }

    protected void pauseApp() {}
    protected void destroyApp(boolean unconditional) { saveFavorites(); }
}
