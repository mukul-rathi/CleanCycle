#include <iostream>
#include <cstring>
#include <bitset>
#include <stdio.h>
#include "encode.h"

// used to test the functions declared in encode.h
int main(void) {

  int numbers1[8] = {89477820, 89478092, 89478832, 89477204, 89477950, 89477159, 89478252, 89478806};
  float fnums1[8] = {10.666587352752686, 10.666619777679443, 10.666707992553711, 10.666513919830322, 10.666602849960327, 10.666508555412292, 10.666638851165771, 10.666704893112183};
  int numbers2[8] = {89477639, 89477972, 89477530, 89479021, 89478082, 89477445, 89477206, 89477298};
  float fnums2[8] = {10.666565775871277, 10.666605472564697, 10.666552782058716, 10.666730523109436, 10.666618585586548, 10.666542649269104, 10.666514158248901, 10.66652512550354};
  int numbers3[8];
  float fnums3[8] = {800, 779, 830, 835, 799, 795, 700, 700};
  int numbers4[8];
  float fnums4[8] = {124, 135, 120, 80, 85, 79, 65, 70};
  unsigned char res[51];

  std::cout << "test" << std::endl;


  for (int i = 0; i < 8; i++) {
    std::cout << std::bitset<32>(numbers1[i]) << std::endl;
    std::cout << std::bitset<32>(numbers2[i]) << std::endl;
    std::cout << std::bitset<32>(numbers3[i]) << std::endl;
    std::cout << std::bitset<32>(numbers4[i]) << std::endl;
  }


  encode_LE(fnums1, fnums2, fnums3, fnums4, res);

  std::cout << "1234567890123456789012345678901234567890123456789" << std::endl;
  for (int i = 0; i < 51; i++) {
    std::cout << std::bitset<8>(res[i]);
    if (i%6 == 5) {
      std::cout << std::endl;
    }
  }
  std::cout << std::endl;


  /*
  for (int i = 0; i < 8; i++) {
    printf("%.10f\n", fnums1[i]);
    printf("%.10f\n", intToLatlong(latlongToInt(fnums1[i]))-180);
  }
  */
}
