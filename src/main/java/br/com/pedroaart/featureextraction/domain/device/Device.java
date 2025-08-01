package br.com.pedroaart.featureextraction.domain.device;

import br.com.pedroaart.featureextraction.controllers.device.DeviceDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "devices")
@Getter
@Setter
@NoArgsConstructor
public class Device {
    @Id
    private String id;
    private String androidId;

    public Device(DeviceDTO deviceDTO) {
        this.androidId = deviceDTO.bluetoothAddress();
    }
}
