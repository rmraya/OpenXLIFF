var button_prefix = 'button_';

/**
 * Returns an element in the current HTML document.
 *
 * @param elementID Identifier of HTML element
 * @return               HTML element object
 */
function getElementObject(elementID) {
    var elemObj = null;
    if (document.getElementById) {
        elemObj = document.getElementById(elementID);
    }
    return elemObj;
}

/**
 * Switches the state of a collapseable box, e.g.
 * if it's opened, it'll be closed, and vice versa.
 *
 * @param boxID Identifier of box
 */
function switchState(boxID) {
    var boxObj = getElementObject(boxID);
    var buttonObj = getElementObject(button_prefix + boxID);
    if (boxObj == null || buttonObj == null) {
        // Box or button not found
    } else if (boxObj.style.display == "none") {
        // Box is closed, so open it
        openBox(boxObj, buttonObj);
    } else if (boxObj.style.display == "block") {
        // Box is opened, so close it
        closeBox(boxObj, buttonObj);
    }
}

/**
 * Opens a collapseable box.
 *
 * @param boxObj       Collapseable box
 * @param buttonObj Button controlling box
 */
function openBox(boxObj, buttonObj) {
    if (boxObj == null || buttonObj == null) {
        // Box or button not found
    } else {
        // Change 'display' CSS property of box
        boxObj.style.display = "block";
        
        // Change text of button
        if (boxObj.style.display == "block") {
            buttonObj.src = "files/general/buttonMinus.gif";
        }
    }
}

/**
 * Closes a collapseable box.
 *
 * @param boxObj       Collapseable box
 * @param buttonObj Button controlling box
 */
function closeBox(boxObj, buttonObj) {
    if (boxObj == null || buttonObj == null) {
        // Box or button not found
    } else {
        // Change 'display' CSS property of box
        boxObj.style.display = "none";
        
        // Change text of button
        if (boxObj.style.display == "none") {
            buttonObj.src = "files/general/buttonPlus.gif";
        }
    }
}