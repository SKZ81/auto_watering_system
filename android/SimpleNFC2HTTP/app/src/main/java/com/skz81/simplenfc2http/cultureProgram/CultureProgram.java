package com.skz81.simplenfc2http.cultureProgram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CultureProgram {

    private String name;
    private List<CulturePhase> phases;

    public CultureProgram() {
        this("", new ArrayList<>());
    }

    public CultureProgram(String name, List<CulturePhase> phases) {
        setName(name);
        setPhases(phases);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public List<CulturePhase> getPhases() {
        return Collections.unmodifiableList(phases);
    }

    public void setPhases(List<CulturePhase> phases) {
        if (phases == null) {
            throw new IllegalArgumentException(
                "Phases cannot be null"
            );
        }

        this.phases = new ArrayList<>(phases);
    }

    public int getDurationDays() {
        int duration = 0;

        for (CulturePhase phase : phases) {
            duration += phase.getDurationDays();
        }

        return duration;
    }

    public int getPhaseStartDay(int phaseIndex) {
        if (phaseIndex < 0 || phaseIndex >= phases.size()) {
            throw new IndexOutOfBoundsException(phaseIndex);
        }

        int day = 1;

        for (int i = 0; i < phaseIndex; i++) {
            day += phases.get(i).getDurationDays();
        }

        return day;
    }

    public int getPhaseEndDay(int phaseIndex) {
        return getPhaseStartDay(phaseIndex)
            + phases.get(phaseIndex).getDurationDays() - 1;
    }

    public void addPhase(CulturePhase phase) {
        if (phase == null) {
            throw new IllegalArgumentException(
                "Phase cannot be null"
            );
        }

        phases.add(phase);
    }

    public void removePhase(int index) {
        phases.remove(index);
    }

    public void movePhase(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= phases.size()
                || toIndex < 0 || toIndex >= phases.size()) {
            throw new IndexOutOfBoundsException("Invalid phase index");
        }

        if (fromIndex == toIndex) {
            return;
        }

        CulturePhase phase = phases.remove(fromIndex);
        phases.add(toIndex, phase);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("name", name);

        JSONArray phasesJson = new JSONArray();

        for (CulturePhase phase : phases) {
            phasesJson.put(phase.toJson());
        }

        json.put("phases", phasesJson);

        return json;
    }

    public static CultureProgram fromJson(JSONObject json)
            throws JSONException {

        String name = json.getString("name");

        JSONArray phasesJson = json.getJSONArray("phases");

        List<CulturePhase> phases = new ArrayList<>();

        for (int i = 0; i < phasesJson.length(); i++) {
            phases.add(
                    CulturePhase.fromJson(phasesJson.getJSONObject(i))
            );
        }

        return new CultureProgram(name, phases);
    }
}

// ----------------------------------------------------
// Serialization example :
// ----------------------------------------------------

// try {
//     JSONObject json = cultureProgram.toJson();
//     String jsonString = json.toString();
//
// } catch (JSONException e) {
//     e.printStackTrace();
// }

// ----------------------------------------------------
// Deserialization example
// ----------------------------------------------------

// try {
//     JSONObject json = new JSONObject(jsonString);
//     CultureProgram program = CultureProgram.fromJson(json);
//
// } catch (JSONException | IllegalArgumentException e) {
//     e.printStackTrace();
// }
