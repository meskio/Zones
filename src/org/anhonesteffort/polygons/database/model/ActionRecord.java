package org.anhonesteffort.polygons.database.model;

public class ActionRecord {
  private int id;
  private String name;
  private String description;
  private int polygon_id;
  private boolean run_on_enter;
  private boolean run_on_exit;

  public ActionRecord(int id, String name, String description, int polygon_id) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.polygon_id = polygon_id;

    run_on_enter = false;
    run_on_exit = false;
  }

  public ActionRecord(int id, String name, String description, int polygon_id, boolean run_on_enter, boolean run_on_exit) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.polygon_id = polygon_id;
    this.run_on_enter = run_on_enter;
    this.run_on_exit = run_on_exit;
  }
  
  public int getID() {
    return id;
  }
  
  public String getName() {
    return name;
  }
  
  public String getDescription() {
    return description;
  }

  public void setPolygonId(int polygon_id) {
    this.polygon_id = polygon_id;
  }
  
  public int getPolygonId() {
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
