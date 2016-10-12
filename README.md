
### React Native Location Services Requiring

a React Native module for requiring location services be enabled or turned on

### Installation

1. `npm install react-native-require-location --save`
2. `react-native link react-native-require-location`


### Usage

```
  import LocationServicesRequired from 'react-native-require-location';
  
  LocationServicesRequired.showSettings({
    message: "<h2>Use Location ?</h2>This app wants to change your device settings:<br/><br/>Use GPS, Wi-Fi, and cell network for location<br/><br/>",
    ok: "YES",
    cancel: "NO"
  }).then(success => {
    
    //can attempt to access location now
    
  }).catch(error => {
      
    //handle error
    
  }); 
  
```

### Notes

the package will auto "close" (it will put the app to the background) the app
hinting to the user that location is required to use the app
