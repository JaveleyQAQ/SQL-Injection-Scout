#include <iostream>
#include <string>
#include <sstream>
#include <unordered_map>
#include <cmath>
#include <regex>

// Helper function to remove spaces from a string
inline std::string removeSpaces(const std::string& str) {
    std::string result;
    std::copy_if(str.begin(), str.end(), std::back_inserter(result),
                 [](char c) { return !std::isspace(c); });
    return result;
}

class Complex {
private:
    double real;
    double imag;

public:
    Complex(double r = 0, double i = 0) : real(r), imag(i) {}

    Complex operator+(const Complex& other) const {
        return Complex(real + other.real, imag + other.imag);
    }

    Complex operator-(const Complex& other) const {
        return Complex(real - other.real, imag - other.imag);
    }

    Complex operator*(const Complex& other) const {
        return Complex(real * other.real - imag * other.imag,
                      real * other.imag + imag * other.real);
    }

    Complex operator/(const Complex& other) const {
        double denom = other.real * other.real + other.imag * other.imag;
        if (denom == 0) {
            throw std::runtime_error("Division by zero");
        }
        return Complex((real * other.real + imag * other.imag) / denom,
                      (imag * other.real - real * other.imag) / denom);
    }

    friend std::ostream& operator<<(std::ostream& os, const Complex& c) {
        if (c.real == 0 && c.imag == 0) {
            return os << "0";
        }
        if (c.real != 0) {
            os << c.real;
        }
        if (c.imag != 0) {
            if (c.imag == 1 && c.real != 0) os << " + i";
            else if (c.imag == -1 && c.real != 0) os << " - i";
            else if (c.imag == 1 && c.real == 0) os << "i";
            else if (c.imag == -1 && c.real == 0) os << "-i";
            else {
                if (c.imag > 0 && c.real != 0) os << " + ";
                os << c.imag << "i";
            }
        }
        return os;
    }

    double modulus() const {
        return std::sqrt(real * real + imag * imag);
    }

    Complex conjugate() const {
        return Complex(real, -imag);
    }

    friend class ComplexParser;
};

class ComplexParser {
public:
    static Complex parse(const std::string& str) {
        std::string s = removeSpaces(str);
        if (s.empty()) throw std::runtime_error("Empty input");

        double real = 0, imag = 0;
        bool hasI = s.find('i') != std::string::npos;

        if (!hasI) {
            real = std::stod(s);
            return Complex(real, 0);
        }

        if (s == "i") return Complex(0, 1);
        if (s == "-i") return Complex(0, -1);

        size_t plusPos = s.find('+', 1);
        size_t minusPos = s.find('-', 1);
        size_t splitPos = std::string::npos;

        if (plusPos != std::string::npos) splitPos = plusPos;
        if (minusPos != std::string::npos) splitPos = minusPos;

        if (splitPos != std::string::npos) {
            std::string realPart = s.substr(0, splitPos);
            std::string imagPart = s.substr(splitPos);
            real = std::stod(realPart);
            if (imagPart == "+i" || imagPart == "-i")
                imag = (imagPart[0] == '+') ? 1 : -1;
            else
                imag = std::stod(imagPart.substr(0, imagPart.length() - 1));
        } else {
            s = s.substr(0, s.length() - 1);
            if (s.empty() || s == "+") imag = 1;
            else if (s == "-") imag = -1;
            else imag = std::stod(s);
        }

        return Complex(real, imag);
    }
};

class Calculator {
private:
    std::unordered_map<std::string, Complex> variables;
    
    bool isValidVariableName(const std::string& name) {
        if (name.empty() || name == "i" || 
            !(std::isalpha(name[0]) || name[0] == '_')) return false;
        
        return std::all_of(name.begin(), name.end(), 
                          [](char c) { return std::isalnum(c) || c == '_'; });
    }

public:
    void run() {
        std::string line;
        std::cout << ">>> ";
        while (std::getline(std::cin, line)) {
            if (line.empty()) {
                std::cout << ">>> ";
                continue;
            }

            try {
                std::string command = line.substr(0, line.find('('));
                if (command == "mod") {
                    processModCommand(line);
                } else if (command == "con") {
                    processConCommand(line);
                } else if (line.find('=') != std::string::npos) {
                    processAssignment(line);
                } else {
                    processExpression(line);
                }
            } catch (const std::exception& e) {
                std::cout << "SyntaxError: " << e.what() << std::endl;
            }
            std::cout << ">>> ";
        }
    }

private:
    void processModCommand(const std::string& line) {
        std::string arg = line.substr(4, line.length() - 5);
        Complex result;
        if (variables.find(arg) != variables.end()) {
            result = variables[arg];
        } else {
            result = ComplexParser::parse(arg);
        }
        std::cout << result.modulus() << std::endl;
    }

    void processConCommand(const std::string& line) {
        std::string arg = line.substr(4, line.length() - 5);
        Complex result;
        if (variables.find(arg) != variables.end()) {
            result = variables[arg];
        } else {
            result = ComplexParser::parse(arg);
        }
        std::cout << result.conjugate() << std::endl;
    }

    void processAssignment(const std::string& line) {
        size_t equalsPos = line.find('=');
        std::string varName = line.substr(0, equalsPos);
        varName = removeSpaces(varName);
        
        if (!isValidVariableName(varName)) {
            throw std::runtime_error("invalid syntax");
        }

        std::string rightSide = line.substr(equalsPos + 1);
        if (variables.find(rightSide) != variables.end()) {
            variables[varName] = variables[rightSide];
        } else {
            variables[varName] = ComplexParser::parse(rightSide);
        }
    }

    void processExpression(const std::string& line) {
        if (variables.find(line) != variables.end()) {
            std::cout << variables[line] << std::endl;
        } else {
            try {
                Complex result = ComplexParser::parse(line);
                std::cout << result << std::endl;
            } catch (...) {
                std::cout << "NameError: name '" << line << "' is not defined" << std::endl;
            }
        }
    }
};

int main() {
    Calculator calc;
    calc.run();
    return 0;
}

