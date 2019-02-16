#include "encode.h"


// converts latitudes or longitudes to fixed point values
// the values have the following format:
// BBBBBBBBB.BBBBBBBBBBBBBBBBBBBBBBB
// note that the value has been made positive by adding 180 to it
// so -10 becomes 170 and 50 becomes 230
unsigned int latlongToInt(float input) {
  unsigned int toAdd = 1;
  toAdd <<= 31;
  unsigned int result = 0;

  // make input positive
  input += 180.0;

  // access the float as the byte sequence and get the exponent
  unsigned int *intRep = (unsigned int *)(&input);
  unsigned char *bytes = (unsigned char *)(&input);

  // extract the exponent from the floating point bytes
  char exponent = (bytes[3]<<1) + (bytes[2]>>7);
  exponent += 128;

  // get the whole-number value
  result = (*intRep)<<9; result = result >> 1;
  result += toAdd;

  // shift to get the correct magnitude
  result = result >> (7-exponent);

  return result;
}

// converts from a fixed-point integer to a value of latitude or longitude
// note that the value is decreased by 180 to counteract the positive shifting
// done by the latlongToInt function
float intToLatlong(unsigned int input) {
  int whole = input >> 23;
  float rest = ((float)(input << 9)) / 4294967296.0f;
  return rest + whole - 180;
}

// gets the bit at the specified index from the source byte array
// the bit is in the least significant bit location of the returned byte
unsigned char getBit(unsigned char *src, int index) {
  int byteIndex = index/8;
  int bitIndex = index%8;
  return (src[byteIndex]>>(7-bitIndex)) & 0x01;
}


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
void encode_LE(float *latitude, float *longitude, float *p10, float *p25, unsigned char *result) {
    // the header is actually 34 bits, so we leave
    // the 6 LSBs of the last element empty
    char header[5] = {0, 0, 0, 0, 0};

    // the first 2 bits of this are empty, so we can
    // then OR the first byte of this and last byte of the header
    char datapoints[47];

    // set up the arrays to let us access the bytes in each float
    // set up arrays and convert the input floats into fixed-point integers
    // using the functions above
    // also get unsigned char pointers to the function to get byte-wise access
    // to the data
    int latInts[8];
    unsigned char *latBytes = (unsigned char *)(&latInts);
    int longInts[8];
    unsigned char *longBytes = (unsigned char *)(&longInts);
    int p10Ints[8];
    unsigned char *p10Bytes = (unsigned char *)(&p10Ints);
    int p25Ints[8];
    unsigned char *p25Bytes = (unsigned char *)(&p25Ints);
    for (int i = 0; i < 8; i++) {
      latInts[i] = latlongToInt(latitude[i]);
      longInts[i] = latlongToInt(longitude[i]);
      p10Ints[i] = (int) p10[i];
      p25Ints[i] = (int) p25[i];
    }

    // set up header
    header[0] = latBytes[3];
    header[1] = latBytes[2];
    header[2] = (latBytes[1]>>7)<<7;
    header[2] += longBytes[3]>>1;
    header[3] = longBytes[3]<<7;
    header[3] += longBytes[2]>>1;
    header[4] = longBytes[2]<<7;
    header[4] += (longBytes[1]>>7)<<6;


    // get the rest of the bits from the input into the datapoints array
    int byteIndex = 0;
    int bitIndex = 2;
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 11; j++) {
        datapoints[byteIndex] += getBit(latBytes, (j+9)%16 + 32*i) << (7-bitIndex);
        bitIndex++;
        if (bitIndex > 7) {
          bitIndex = 0;
          byteIndex++;
        }
      }

      for (int j = 0; j < 11; j++) {
        datapoints[byteIndex] += getBit(longBytes, (j+9)%16 + 32*i) << (7-bitIndex);
        bitIndex++;
        if (bitIndex > 7) {
          bitIndex = 0;
          byteIndex++;
        }
      }

      for (int j = 0; j < 10; j++) {
        datapoints[byteIndex] += getBit(p10Bytes, (j+14)%16 + i*32) << (7-bitIndex);
        bitIndex++;
        if (bitIndex > 7) {
          bitIndex = 0;
          byteIndex++;
        }
      }

      for (int j = 0; j < 10; j++) {
        datapoints[byteIndex] += getBit(p25Bytes, (j+14)%16 + i*32) << (7-bitIndex);
        bitIndex++;
        if (bitIndex > 7) {
          bitIndex = 0;
          byteIndex++;
        }
      }
    }

    // copy the data from the header and datapoints arrays
    // into the result array that is passed in
    result[0] = header[0];
    result[1] = header[1];
    result[2] = header[2];
    result[3] = header[3];
    result[4] = header[4] | datapoints[0];
    for (int i = 1; i < 48; i++) {
      result[4+i] = datapoints[i];
    }
}
