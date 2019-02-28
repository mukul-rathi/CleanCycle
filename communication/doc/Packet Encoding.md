## Packet Design
The aim of this design is to maximise the amount of data we can send in each payload, since we have a limited amount of data that we can transfer per day.
We can send 51 bytes (408 bits) with each payload.
We want to send as much necessary data per packet as possible. In order to do this we need to prioritise which data we send and find a space-efficient way of sending it.  
Since the scope of our project encompasses only the pollution aspect of the data, we can limit the data we send over LoRaWAN to only latitude, longitude, PM10 and PM2.5 values.

From the documentation of the SDS011<sup>[1]</sup>, we know that the PM10 and PM2.5 values will be in the range 0-1000. If we truncate the values and remove the decimal point, we can represent this using only 10 bits (2<sup>10</sup> = 1024). This is a reasonable truncation since there is some error in the air quality sensor measurement, and we also don't need overly precise measurements for our end goal.

The next thing we need to find out is how the number of decimal places in the latitude and longitude relates to the precision of our measurement. The following tables show this relation both in decimal and in binary. In our case the binary table is more informative.

### In Decimal<sup>[2]</sup>


| Number of decimals | Precision |
|:------------------:|:---------:|
| 1     (.1)         |    11km   |
| 2     (.01)        |    1.11km |
| 3     (.001)       |    111m   |
| 4     (.0001)      |    11.1m  |
| 5     (.00001)     |    1.11m  |
| 6     (.000001)    |    11cm   |

### In binary:

|    # of bits after fixed point | Decimal Fraction   | Precision  |
| :----------------------------: | :----------------: | :--------- |
|                              2 | 1/4                | 27.8km     |
|                              4 | 1/16               | 6.94km     |
|                              6 | 1/64               | 1.74km     |
|                              8 | 1/256              | 434m       |
|                             10 | 1/1024             | 109m       |
|                             12 | 1/4096             | 27.1m      |
|                             14 | 1/16384            | 6.78m      |
|                             16 | 1/65536            | 1.70m      |
|                             18 | 1/262144           | 424mm      |
|                             19 | 1/524288           | 212mm      |
|                             20 | 1/1048576          | 106mm      |

We are trying to map out pollution on roads, so we would only need our precision to be within ~1m. However, we have decided to be slightly more precise than this in order to allow for more precise data manipulation in the future.  
In the end we have decided to use 19 bits for our binary representation of the data, which is a good compromise between datapoint size and precision.

Latitude and longitude range between -180 and 180, so we have to represent 360 whole numbers before the fixed point, which requires 9 bits (2<sup>9</sup> = 512). We can then use 19 bits after the fixed point in order to get an accuracy of around 200mm. This gives a total of 28 bits for each of the latitude and longitude, so we get a total of 56 bits needed to store the longitude and latitude.

Sending the data this way would lead to using 76 bits per datapoint, so we could fit 5 datapoints per packet, but we want to be able to send more.

## Headers
In order to achieve better compression, we can use the fact that the user will not be travelling very fast. This means that all the latitude/longitude values will be fairly similar in each packet. In particular, we don't expect the most significant bits (MSBs) of the latitude and longitude values to change very much. This means we can put the MSBs of the latitude and longitude in a header that prepends the rest of the message. Subsequent datapoints can then skip the MSBs, and so only take up a part of the space they would take up otherwise.

If we assume the user will travel less than 800m per packet, we can store 17 bits of the latitude and longitude in the header and 11 in each datapoint. Doing this allows for each header to denote a 434m square within which the user can move around with no packet-wise data inconsistency. However, if the user crosses one of the edges of the square, the header value won't update even though it should, so in isolation it would look like the user had "wrapped around" the square and suddenly appeared to be on the opposite side.  
We can deal with this in the decoder by checking the difference between each point and if this distance exceeds half of the width of the square then we assume that they crossed an edge and just increase the least significant bit of the header for that point and all the subsequent ones.  
It would be possible to put more of the bits in the header and fewer in the body of the packets, but this could lead to inconsistent data if a user moves very fast, so it was decided to compress more conservatively and have better assurances that the data is consistent.
/////// FOR THIS SECTION, A DIAGRAM WITH SQUARES WHERE SQUARE SIDES ARE THE /////// CORRECT LENGTH MIGHT BE USEFUL

It is possible to apply the same system for the PM10 and PM2.5 readings, but with an area as large as 434m it is possible that there are very high pollution differentials, and since the core principle of our project is to avoid high-pollution areas, we want to know if there are areas which have extremely high spikes of pollution, and this could be missed if we used the same system.

One other possible way of compressing the data about the PM10 and PM2.5 readings is to instead send the index of the most significant high bit of the binary value, with 0 meaning that either there are no high bits or only the LSB is high.
For example, for a reading of b10 1110 1011 we would send 9 and for a reading of b00 1011 1111 we would send 7
The values would then represent the following ranges:

| Value sent | PM range |
| ----------:| -------- |
|          0 |      0-1 |
|          1 |      2-3 |
|          2 |      4-7 |
|          3 |     8-15 |
|          4 |    16-31 |
|          5 |    32-63 |
|          6 |   64-127 |
|          7 |  128-255 |
|          8 |  256-511 |
|          9 | 512-1023 |
|            |          |

This would take us from needing 10 bits to needing 4 bits. This is a fairly good amount of compression, but the amount of data that is lost is very significant, so it was decided that this is not worthwhile

## Summary
We end up with a header containing 34 bits, and each subsequent datapoint will need 42 bits of data. This allows us to fit 8 datapoints per packet and leaving 34 bits free at the end for potentially doing some sort of checksum or adding other miscellaneous packet data. 
This covers at most 24 seconds of movement, in which a cyclist travelling at 15m/s (54km/h) would travel 360m, which is less than the precision where we are splitting the data, so this does not cause issues.



[[1]: https://nettigo.pl/attachments/398
[1] <https://nettigo.pl/attachments/398>  
[[2]: https://en.wikipedia.org/wiki/Decimal_degrees
[2] <https://en.wikipedia.org/wiki/Decimal_degrees>
