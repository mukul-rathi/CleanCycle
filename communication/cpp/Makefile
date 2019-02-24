CXX = g++
make:
	$(shell mkdir -p ../build)
	$(CXX) encode.cpp -c -o ../build/encode
test: make
	$(CXX) encode.cpp test.cpp -o ../build/test
	$(shell ../build/test > ../encoded.txt)
