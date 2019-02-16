
// converts latitudes or longitudes to fixed point values
// the values have the following format:
// BBBBBBBBB.BBBBBBBBBBBBBBBBBBBBBBB
// note that the value has been made positive by adding 180 to it
// so -10 becomes 170 and 50 becomes 230
unsigned int latlongToInt(float input);

// converts from a fixed-point integer to a value of latitude or longitude
// note that the value is decreased by 180 to counteract the positive shifting
// done by the latlongToInt function
float intToLatlong(unsigned int input);

// gets the bit at the specified index from the source byte array
// the bit is in the least significant bit location of the returned byte
unsigned char getBit(unsigned char *src, int index);

/*
// this function takes in an array of latitude values,
//                        an array of longitude values,
//                        an array of PM10 values,
//                        an array of PM25 values, and
//                        a pointer to the results array
// each of the latitude, longitude, PM10 and PM25 arrays has to
// be 8 elements in length, and the result array must be at least
// 51 bytes in length
// THIS FUNCTION WORKS ON LITTLE-ENDIAN SYSTEMS
*/
void encode_LE(float *latitude,
               float *longitude,
               float *p10,
               float *p25,
               unsigned char *result);
