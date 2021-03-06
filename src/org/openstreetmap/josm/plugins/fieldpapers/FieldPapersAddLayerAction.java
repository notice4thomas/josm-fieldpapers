package org.openstreetmap.josm.plugins.fieldpapers;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import static org.openstreetmap.josm.tools.I18n.tr;

@SuppressWarnings("serial")
public class FieldPapersAddLayerAction extends JosmAction {

    public FieldPapersAddLayerAction() {
        super(tr("Scanned Map..."), "fieldpapers",
            tr("Display a map that was previously scanned and uploaded to fieldpapers.org"), null, false);
    }

    public void actionPerformed(ActionEvent e) {
        String url = JOptionPane.showInputDialog(Main.parent,
            tr("Enter a fieldpapers.org snapshot URL"),
                Main.pref.get("fieldpapers.last-used-id"));

        if (url == null || url.equals("")) return;

        if (!url.startsWith("http")) {
            url = Main.pref.get("fieldpapers-base-url", "http://fieldpapers.org/") + "snapshots/" + url;
        }

        try {
            // fetch metadata
            JsonObject metadata = getMetadata(url);

            String tileJsonUrl = metadata.getJsonString("tilejson_url").getString();
            String id = metadata.getJsonString("id").getString();

            // fetch TileJSON
            JsonObject tileJson = getTileJson(tileJsonUrl);

            String tileUrl = tileJson.getJsonArray("tiles").getString(0);
            int minZoom = tileJson.getJsonNumber("minzoom").intValue();
            int maxZoom = tileJson.getJsonNumber("maxzoom").intValue();

            JsonArray bounds = tileJson.getJsonArray("bounds");
            double west = bounds.getJsonNumber(0).doubleValue();
            double south = bounds.getJsonNumber(1).doubleValue();
            double east = bounds.getJsonNumber(2).doubleValue();
            double north = bounds.getJsonNumber(3).doubleValue();

            // save this atlas as the
            Main.pref.put("fieldpapers.last-used-id", id);

            Bounds b = new Bounds(new LatLon(south, west), new LatLon(north, east));

            FieldPapersLayer wpl = new FieldPapersLayer(id, tileUrl, b, minZoom, maxZoom);
            Main.main.addLayer(wpl);

        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(Main.parent,tr("Could not read information for the id \"{0}\" from fieldpapers.org", url));
        }
    }

    private JsonObject getMetadata(String snapshotUrl) throws IOException {
        InputStream is = null;
        JsonReader reader = null;

        try {
            URL url = new URL(snapshotUrl);

            URLConnection connection = url.openConnection();
            connection.setRequestProperty("Accept", "application/json");

            is = connection.getInputStream();
            reader = Json.createReader(is);

            return reader.readObject();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException err) {
                    // Ignore
                }
            }
            if (reader != null) {
                reader.close();
            }
        }
    }

    private JsonObject getTileJson(String tileJsonUrl) throws IOException {
        InputStream is = null;
        JsonReader reader = null;

        try {
            URL url = new URL(tileJsonUrl);

            is = url.openStream();
            reader = Json.createReader(is);

            return reader.readObject();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException err) {
                    // Ignore
                }
            }
            if (reader != null) {
                reader.close();
            }
        }
    }
}
