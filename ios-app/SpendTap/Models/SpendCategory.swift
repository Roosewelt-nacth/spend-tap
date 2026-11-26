import AppIntents

/// Category list shown by the Back Tap shortcut and the app.
/// The first four are the ones requested; the rest are proposals (see PLAN-ios-backtap.md).
enum SpendCategory: String, AppEnum, CaseIterable {
    case family, friends, personal, petrol
    case food, groceries, transport, shopping, bills, health, entertainment, other

    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Category"

    static var caseDisplayRepresentations: [SpendCategory: DisplayRepresentation] = [
        .family: DisplayRepresentation(title: "Family", image: .init(systemName: "house.fill")),
        .friends: DisplayRepresentation(title: "Friends", image: .init(systemName: "person.2.fill")),
        .personal: DisplayRepresentation(title: "Personal", image: .init(systemName: "person.fill")),
        .petrol: DisplayRepresentation(title: "Petrol", image: .init(systemName: "fuelpump.fill")),
        .food: DisplayRepresentation(title: "Food & Dining", image: .init(systemName: "fork.knife")),
        .groceries: DisplayRepresentation(title: "Groceries", image: .init(systemName: "cart.fill")),
        .transport: DisplayRepresentation(title: "Transport", image: .init(systemName: "bus.fill")),
        .shopping: DisplayRepresentation(title: "Shopping", image: .init(systemName: "bag.fill")),
        .bills: DisplayRepresentation(title: "Bills & Rent", image: .init(systemName: "doc.text.fill")),
        .health: DisplayRepresentation(title: "Health", image: .init(systemName: "cross.case.fill")),
        .entertainment: DisplayRepresentation(title: "Entertainment", image: .init(systemName: "film.fill")),
        .other: DisplayRepresentation(title: "Other", image: .init(systemName: "ellipsis.circle.fill")),
    ]

    var title: String {
        String(localized: Self.caseDisplayRepresentations[self]!.title)
    }

    var symbol: String {
        switch self {
        case .family: "house.fill"
        case .friends: "person.2.fill"
        case .personal: "person.fill"
        case .petrol: "fuelpump.fill"
        case .food: "fork.knife"
        case .groceries: "cart.fill"
        case .transport: "bus.fill"
        case .shopping: "bag.fill"
        case .bills: "doc.text.fill"
        case .health: "cross.case.fill"
        case .entertainment: "film.fill"
        case .other: "ellipsis.circle.fill"
        }
    }
}
