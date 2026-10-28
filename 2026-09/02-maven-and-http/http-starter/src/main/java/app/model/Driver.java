package app.model;

public class Driver {
    private String id;
    private String name;
    private String email;
    private int age;
    private int experience;
    private boolean hasDeposit;
    private String carId;

    public Driver() {}

    public Driver(String id, String name, String email, int age, int experience, boolean hasDeposit) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
        this.experience = experience;
        this.hasDeposit = hasDeposit;
        this.carId = null;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }
    public boolean isHasDeposit() { return hasDeposit; }
    public void setHasDeposit(boolean hasDeposit) { this.hasDeposit = hasDeposit; }
    public String getCarId() { return carId; }
    public void setCarId(String carId) { this.carId = carId; }
}
