import XCTest

final class AndroidAdvertTests: XCTestCase {
    func testTruncatedNameAndSize() {
        let nickname = "SuperLongNickName"
        let advName = String(nickname.prefix(7))
        let uuidBytes = 16
        let size = 3 + 18 + 2 + advName.utf8.count
        XCTAssertEqual(advName, "SuperLo")
        XCTAssertLessThanOrEqual(size, 31)
    }
}
