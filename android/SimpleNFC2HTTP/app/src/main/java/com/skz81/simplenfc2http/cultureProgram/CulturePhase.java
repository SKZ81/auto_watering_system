package com.skz81.simplenfc2http.cultureProgram;

import androidx.annotation.NonNull;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CulturePhase {

    private String name;
    private int durationDays;
    private double[] fertilizerDosages;

    public CulturePhase() {
        this("", 1, new double[0]);
    }

    public CulturePhase(String name, int durationDays,
                        double[] fertilizerDosages) {
        setName(name);
        setDurationDays(durationDays);
        setFertilizerDosages(fertilizerDosages);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        if (durationDays <= 0) {
            throw new IllegalArgumentException(
                "Phase duration must be positive"
            );
        }
        this.durationDays = durationDays;
    }

    public double[] getFertilizerDosages() {
        return fertilizerDosages.clone();
    }

    public void setFertilizerDosages(double[] dosages) {
        if (dosages == null) {
            throw new IllegalArgumentException(
                "Fertilizer dosages cannot be null"
            );
        }

        for (double dosage : dosages) {
            if (!Double.isFinite(dosage) || dosage < 0) {
                throw new IllegalArgumentException(
                    "Invalid fertilizer dosage: " + dosage
                );
            }
        }

        this.fertilizerDosages = dosages.clone();
    }

    public double getFertilizerDosage(int index) {
        return fertilizerDosages[index];
    }

    public void setFertilizerDosage(int index, double dosage) {
        if (!Double.isFinite(dosage) || dosage < 0) {
            throw new IllegalArgumentException(
                "Invalid fertilizer dosage"
            );
        }
        fertilizerDosages[index] = dosage;
    }

    @NonNull
    @Override
    public String toString() {
        return "CulturePhase{" +
            "name='" + name + '\'' +
            ", durationDays=" + durationDays +
            ", fertilizerDosages=" +
            Arrays.toString(fertilizerDosages) +
            '}';
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("name", name);
        json.put("durationDays", durationDays);

        JSONArray dosages = new JSONArray();

        for (double dosage : fertilizerDosages) {
            dosages.put(dosage);
        }

        json.put("fertilizerDosages", dosages);

        return json;
    }

    public static CulturePhase fromJson(JSONObject json)
            throws JSONException {

        String name = json.getString("name");
        int durationDays = json.getInt("durationDays");

        JSONArray dosagesJson = json.getJSONArray("fertilizerDosages");
        double[] dosages = new double[dosagesJson.length()];

        for (int i = 0; i < dosages.length; i++) {
            dosages[i] = dosagesJson.getDouble(i);
        }

        return new CulturePhase(name, durationDays, dosages);
    }
}
