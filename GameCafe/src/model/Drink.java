package model;

import java.util.List;

public class Drink {
    private String name;
    private List<String> ingredients;

    public Drink(String name, List<String> ingredients) {
        this.name = name;
        this.ingredients = ingredients;
    }
}