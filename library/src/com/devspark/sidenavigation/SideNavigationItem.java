package com.devspark.sidenavigation;

/**
 * Item of side navigation.
 * 
 * @author johnkil
 * 
 */
class SideNavigationItem {

    private int id;
    private int type;
    private String text;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
