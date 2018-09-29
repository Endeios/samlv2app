package web.domain;

import java.util.Objects;

/**
 * An Item
 */
public class Item {
    private final long id;
    private final String description;
    private final String value;

    /**
     * Creates an {@link Item}
     *
     * @param id
     *         the id of the item
     * @param description
     *         the description
     * @param value
     *         the value
     */
    public Item(long id, String description, String value) {
        this.id = id;
        this.description = description;
        this.value = value;
    }

    public Item(Object[] item) {
        this((long) item[0], (String) item[1], (String) item[2]);
    }

    public long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Item)) {
            return false;
        }
        Item item = (Item) o;
        return getId() == item.getId() &&
                Objects.equals(getDescription(), item.getDescription()) &&
                Objects.equals(getValue(), item.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getDescription(), getValue());
    }

    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
