package com.example.admin.rxjavatestapplication.model;

import java.util.ArrayList;
import java.util.List;

public class Tracks {

    private String offset;

    private Integer total;

    private List<Item> items;

    public List<Item> getItems() {
        return items;
    }

    public Tracks(List<Item> items, String offset, Integer total) {
        this.items = items;
        this.offset = offset;
        this.total = total;
    }

    public String getOffset() {
        return offset;
    }


    public Integer getTotal() {
        return total;
    }
}
