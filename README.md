# RAtel
The telemetry assignment is run from the Maim class.
It's main method will first update the data that is located in the resources folder.
The finalData123.json file has a list of TelemetrySamples for the 3 different vehicles.
The data is created using excel and it is easier to visualise the data in the generatingData.xlsx file.

After updating the data so that it's timestamps start 2s from the moment the program is started, it will create gui and update the data for vehicles in the backgroud.
If search button is pressed with an empty searchbox it will list all the vehicles bu id and calculated data can be shown for the selected vehicle.
Message box is not updated because it would be confusing, so in order to see the updated data, it needs to be shown again.
The created data is loaded during a period of around 30s, and the vehicle with an ID of "1" starts with 0km traveled so the average speed stat changes quite a bit.
