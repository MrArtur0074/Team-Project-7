package com.foodmaster.foodmasterbot;

public class UserData {
    private int height;
    private int weight;
    private int age;
    private String gender; // "male" или "female"
    private String activityLevel; // уровень активности

    public void setHeight(int height) { this.height = height; }
    public void setWeight(int weight) { this.weight = weight; }
    public void setAge(int age) { this.age = age; }
    public void setGender(String gender) { this.gender = gender; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }

    public int getHeight() { return height; }
    public int getWeight() { return weight; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getActivityLevel() { return activityLevel; }
}
