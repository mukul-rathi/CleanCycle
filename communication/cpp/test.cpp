#include <iostream>
#include <fstream>
#include <cstring>
#include <bitset>
#include "encode.h"
#include <boost/algorithm/string.hpp>

// used to test the functions declared in encode.h
int main(void) {
  float testNumsLat[1024];
  float testNumsLong[1024];
  float testNumsPm10[1024];
  float testNumsPm25[1024];
  unsigned char encoded[51*(1024/8)];

  std::ifstream file;
  file.open("../inputs.txt");

  if (!file) {
    std::cout << "error opening file" << std::endl;
    return -1;
  }

  std::string line;
  std::vector<std::string> lineSplit;
  int counter = 0;
  while (getline(file, line) && counter < 1024) {
    lineSplit = boost::split(lineSplit, line, [](char c){ return c == ',';});
    testNumsLat[counter] = std::stof(lineSplit[0]);
    testNumsLong[counter] = std::stof(lineSplit[1]);
    testNumsPm10[counter] = std::stof(lineSplit[2]);
    testNumsPm25[counter] = std::stof(lineSplit[3]);
    counter++;
  }

  for (int i = 0; i < 1024/8; i++) {
    encode(&testNumsLat[8*i],
           &testNumsLong[8*i],
           &testNumsPm10[8*i],
           &testNumsPm25[8*i],
           &encoded[51*i]);

    for (int j = 0; j < 51; j++) {
        std::cout << std::bitset<8>(encoded[51*i + j]);
    }
    std::cout << std::endl;
  }
}
