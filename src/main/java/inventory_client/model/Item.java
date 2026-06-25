package inventory_client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {
    private Long id;
    private String name;
    private String category;
    private String colorType;
    private String boxNumber;
    private Integer quantity;
    private String description;
    private String printerName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate supplyDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getColorType() { return colorType; }
    public void setColorType(String colorType) { this.colorType = colorType; }

    public String getBoxNumber() { return boxNumber; }
    public void setBoxNumber(String boxNumber) { this.boxNumber = boxNumber; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getSupplyDate() { return supplyDate; }
    public void setSupplyDate(LocalDate supplyDate) { this.supplyDate = supplyDate; }

    public String getPrinterName() { return printerName; }
    public void setPrinterName(String printerName) { this.printerName = printerName; }
}