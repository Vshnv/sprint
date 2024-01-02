let bookARoom = {(currentState)
    print("Select : " + currentState)
    let selection = input()
    let res = currentState@filter({(e) e != selection})
    print(res)
    res
}

let hotelBookingMenu = {(currentState)
    print("==== Hotel Menu ====")
    print("1. Book a room")
    print("2. See available rooms")
    print("3. Quit")
    let selection = input()
    if selection == "1" {
        hotelBookingMenu(bookARoom(currentState))
    } else if selection == "2" {
        print("Rooms: " + currentState)
        hotelBookingMenu(currentState)
    } else if selection == "3" {
        print("Bye")
    } else {
        print("Invalid Input!")
        hotelBookingMenu(currentState)
    }
}

let for = {(operand, conditionFunction, update, operation)
    if conditionFunction(operand) {
        operation(operand)
        for(update(operand), conditionFunction, update, operation)
    }
}

for(0, {(c) c<10}, {(u) u + 1}) {(e)
    print(e)
}
