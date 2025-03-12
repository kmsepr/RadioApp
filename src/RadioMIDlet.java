import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.media.*;
import javax.microedition.rms.*;
import java.util.Vector;

public class RadioMIDlet extends MIDlet implements CommandListener {
    private Display display;
    private List mainList;
    private Command exitCommand, addCommand, deleteCommand, playCommand, backCommand, saveCommand;
    private Form addForm;
    private TextField urlField, nameField;
    private Vector favorites;
    private Player player; // Media Player for streaming

    public RadioMIDlet() {
        display = Display.getDisplay(this);
        favorites = new Vector();
        loadFavorites();
    }

    protected void startApp() {
        mainList = new List("Internet Radio", List.IMPLICIT);
        exitCommand = new Command("Exit", Command.EXIT, 1);
        addCommand = new Command("Add Station", Command.SCREEN, 1);
        deleteCommand = new Command("Delete", Command.ITEM, 2);
        playCommand = new Command("Play", Command.ITEM, 1);
        backCommand = new Command("Back", Command.BACK, 1);
        saveCommand = new Command("Save", Command.OK, 1);

        mainList.addCommand(exitCommand);
        mainList.addCommand(addCommand);
        mainList.addCommand(deleteCommand);
        mainList.addCommand(playCommand);
        mainList.setCommandListener(this);

        // Create 'Add Station' form
        addForm = new Form("Add Radio Station");
        nameField = new TextField("Station Name:", "", 32, TextField.ANY);
        urlField = new TextField("Stream URL:", "", 256, TextField.URL);
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

    private void loadFavorites() {
        try {
            RecordStore rs = RecordStore.openRecordStore("favorites", true);
            if (rs.getNumRecords() > 0) {
                byte[] raw = rs.getRecord(1);
                String data = new String(raw);
                String[] stations = split(data, "\n");
                for (int i = 0; i < stations.length; i++) {
                    String[] parts = split(stations[i], "|");
                    if (parts.length == 2) {
                        favorites.addElement(parts);
                    }
                }
            }
            rs.closeRecordStore();
        } catch (Exception e) {
            Alert alert = new Alert("Notice", "Welcome to Internet Radio", null, AlertType.INFO);
            display.setCurrent(alert, mainList);
        }
    }

    private void saveFavorites() {
        try {
            RecordStore.deleteRecordStore("favorites");
            RecordStore rs = RecordStore.openRecordStore("favorites", true);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < favorites.size(); i++) {
                String[] station = (String[]) favorites.elementAt(i);
                sb.append(station[0]).append("|").append(station[1]).append("\n");
            }
            byte[] raw = sb.toString().getBytes();
            rs.addRecord(raw, 0, raw.length);
            rs.closeRecordStore();

            Alert alert = new Alert("Success", "Station saved successfully", null, AlertType.CONFIRMATION);
            alert.setTimeout(Alert.FOREVER);
            display.setCurrent(alert, mainList);
        } catch (Exception e) {
            Alert alert = new Alert("Error", "Could not save stations", null, AlertType.ERROR);
            alert.setTimeout(Alert.FOREVER);
            display.setCurrent(alert, mainList);
        }
    }

    private void deleteStation(int index) {
        try {
            favorites.removeElementAt(index);
            saveFavorites();
            updateList();
            Alert alert = new Alert("Success", "Station deleted", null, AlertType.CONFIRMATION);
            alert.setTimeout(Alert.FOREVER);
            display.setCurrent(alert, mainList);
        } catch (Exception e) {
            Alert alert = new Alert("Error", "Could not delete station", null, AlertType.ERROR);
            alert.setTimeout(Alert.FOREVER);
            display.setCurrent(alert, mainList);
        }
    }

    private String[] split(String str, String sep) {
        Vector parts = new Vector();
        int idx;
        while ((idx = str.indexOf(sep)) != -1) {
            parts.addElement(str.substring(0, idx));
            str = str.substring(idx + sep.length());
        }
        parts.addElement(str);
        String[] result = new String[parts.size()];
        parts.copyInto(result);
        return result;
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
            if (idx >= 0) {
                deleteStation(idx);
            }
        } else if (c == playCommand) {
            int idx = mainList.getSelectedIndex();
            if (idx >= 0) {
                playStream(idx);
            }
        } else if (c == saveCommand && d == addForm) {
            String name = nameField.getString().trim();
            String url = urlField.getString().trim();
            if (name.length() > 0 && url.length() > 0) {
                favorites.addElement(new String[]{name, url});
                saveFavorites();
                updateList();
                display.setCurrent(mainList);
            } else {
                Alert alert = new Alert("Error", "Please fill all fields", null, AlertType.ERROR);
                alert.setTimeout(Alert.FOREVER);
                display.setCurrent(alert, addForm);
            }
        }
    }

    private void playStream(int index) {
        try {
            String[] station = (String[]) favorites.elementAt(index);

            if (player != null) {
                player.close(); // Stop previous stream
            }

            player = Manager.createPlayer(station[1], "audio/mpeg");
            player.realize();
            player.prefetch();
            player.start();

            Alert playingAlert = new Alert("Playing", "Now Playing: " + station[0], null, AlertType.INFO);
            playingAlert.setTimeout(Alert.FOREVER);
            display.setCurrent(playingAlert, mainList);
        } catch (Exception e) {
            Alert alert = new Alert("Error", "Could not play station: " + e.getMessage(), null, AlertType.ERROR);
            alert.setTimeout(Alert.FOREVER);
            display.setCurrent(alert, mainList);
        }
    }

    protected void pauseApp() {}

    protected void destroyApp(boolean unconditional) {
        if (player != null) {
            try {
                player.close();
            } catch (Exception e) {}
        }
        saveFavorites();
    }
        }
