package org.anhonesteffort.polygons.storage;

public class ActionBroadcastRecord {
  private int id;
  private String name;
  private String description;
  private int polygon_id;
  private boolean run_on_enter;
  private boolean run_on_exit;

  public ActionBroadcastRecord(int id, String name, String description, int polygon_id, boolean run_on_enter, boolean run_on_exit) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.polygon_id = polygon_id;
    this.run_on_enter = run_on_enter;
    this.run_on_exit = run_on_exit;
  }
  
  public void setID(int id) {
    this.id = id;
  }
  
  public int getID() {
    return id;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getName() {
    return name;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setPolygonID(int polygon_id) {
    this.polygon_id = polygon_id;
  }
  
  public int getPolygonID() {
    return polygon_id;
  }
  
  public void setRunOnEnter(boolean run_on_enter) {
    this.run_on_enter = run_on_enter;
  }
  
  public boolean runOnEnter() {
    return run_on_enter;
  }
  
  public void setRunOnExit(boolean run_on_exit) {
    this.run_on_exit = run_on_exit;
  }
  
  public boolean runOnExit() {
    return run_on_exit;
  }
  
}
