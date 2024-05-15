package amie.mining.utils;

import amie.data.remote.Caching;
import amie.data.AbstractKB;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Every possible options for AMIE.
 * TODO: refactoring default values as constant formated into descriptions like cache options
 */
public interface AMIEOptions {

    Option CACHE = new Option("cache", false, "Enables query caching.");
    Option CSIZE = new Option("csize", true,
            String.format("Enables cache and set cache size ; default %s queries", Caching.DEFAULT_CACHE_SIZE));
    Option CPOL = new Option("cpol", true,
            String.format("Enables cache and set cache policy ; default %s policy", Caching.DEFAULT_POLICY));
    Option REMOTE_KB_MODE_SERVER = new Option("server", true,
            String.format("Enables server mode (layer type as arg ; default is %s)",
                    AbstractKB.GetDefaultCommunicationLayerType()));
    Option REMOTE_KB_MODE_CLIENT = new Option("client", true,
            String.format("Enables client mode (layer type as arg ; default is %s)",
                    AbstractKB.GetDefaultCommunicationLayerType()));
    Option SERVER_ADDRESS = new Option("serverAddress", true,
            String.format("Enables client mode and sets server address (default is %s)",
                    AbstractKB.DEFAULT_SERVER_ADDRESS));
    Option PORT = new Option("port", true,
            String.format("Enables server mode with default layer type (%s) and sets port (default is %s)",
                    AbstractKB.GetDefaultCommunicationLayerType(),
                    AbstractKB.DEFAULT_PORT));

    Option MIN_SUPPORT = new Option("mins", "min-support", true,
            "Minimum absolute support. Default: 100 positive examples");

    Option MIN_INITIAL_SUPPORT = new Option("minis", "min-initial-support", true,
            "Minimum size of the relations to be considered as head relations. Default: 100 (facts or " +
                    "entities depending on the bias)");

    Option MIN_HEAD_COVERAGE = new Option("minhc", "min-head-coverage", true,
            "Minimum head coverage. Default: 0.01");

    Option PRUNING_METRIC = new Option("pm", "pruning-metric", true,
            "Metric used for pruning of intermediate queries: support|headcoverage. Default: " +
                    "headcoverage");

    Option OUTPUT_AT_END = new Option("oute", "output-at-end", false,
            "Print the rules at the end and not while they are discovered. Default: false");

    Option OUTPUT_FORMAT = new Option("ofmt", "output-format", true,
            "Controls the rules' output format. Default: TSV, all fields, atoms as triples");

    Option BODY_EXCLUDED = new Option("bexr", "body-excluded-relations", true,
            "Do not use these relations as atoms in the body of rules. Example: <livesIn>,<bornIn>");

    Option HEAD_EXCLUDED = new Option("hexr", "head-excluded-relations", true,
            "Do not use these relations as atoms in the head of rules (incompatible with " +
                    "head-target-relations). Example: <livesIn>,<bornIn>");

    Option INSTANTIATION_EXCLUDED = new Option("iexr", "instantiation-excluded-relations", true,
            "Do not instantiate these relations. Should be used with -fconst or -const (incompatible with" +
                    " instantiation-target-relations). Example: <livesIn>,<bornIn>");

    Option HEAD_TARGET_RELATIONS = new Option("htr", "head-target-relations", true,
            "Mine only rules with these relations in the head. Provide a list of relation names separated " +
                    "by commas (incompatible with head-excluded-relations). Example: <livesIn>,<bornIn>");

    Option BODY_TARGET_RELATIONS = new Option("btr", "body-target-relations", true,
            "Allow only these relations in the body. Provide a list of relation names separated by commas " +
                    "(incompatible with body-excluded-relations). Example: <livesIn>,<bornIn>");

    Option INSTANTIATION_TARGET_RELATIONS = new Option("itr", "instantiation-target-relations", true,
            "Allow only these relations to be instantiated. Should be used with -fconst or -const. " +
                    "Provide a list of relation names separated by commas (incompatible with " +
                    "instantiation-excluded-relations). Example: <livesIn>,<bornIn>");

    Option MAX_DEPTH = new Option("maxad", "max-depth", true,
            "Maximum number of atoms in the antecedent and succedent of rules. Default: 3");

    Option MIN_PCA_CONFIDENCE = new Option("minpca", "min-pca-confidence", true,
            "Minimum PCA confidence threshold. This value is not used for pruning, only for filtering of the " +
                    "results. Default: 0.0");

    Option ALLOW_CONSTANTS = new Option("const", "allow-constants", false,
            "Enable rules with constants. Default: false");


    Option ONLY_CONSTANTS = new Option("fconst", "only-constants", false,
            "Enforce constants in all atoms. Default: false");


    Option BIAS = new Option("bias", "e-name", true,
            "Syntatic/semantic bias: oneVar|default|lazy|lazit|[Path to a subclass of " +
                    "amie.mining.assistant.MiningAssistant] Default: default (defines support and confidence in terms " +
                    "of 2 head variables given an order, cf -vo)");


    Option COUNT_ALWAYS_ON_SUBJECT = new Option("caos", "count-always-on-subject", false,
            "If a single variable bias is used (oneVar), force to count support always on the subject " +
                    "position.");


    Option N_THREADS = new Option("nc", "n-threads", true,
            "Preferred number of cores. Round down to the actual number of cores in the system if a " +
                    "higher value is provided.");


    Option MIN_STD_CONFIDENCE = new Option("minc", "min-std-confidence", true,
            "Minimum standard confidence threshold. "
                    + "This value is not used for pruning, only for filtering of the results. Default: 0.0");


    Option OPTIM_CONFIDENCE_BOUNDS = new Option("optimcb", "optim-confidence-bounds", false,
            "Enable the calculation of confidence upper bounds to prune rules.");


    Option OPTIM_FUNC_HEURISTIC = new Option("optimfh", "optim-func-heuristic", false,
            "Enable functionality heuristic to identify potential low confident rules for pruning.");


    Option VERBOSE = new Option("verbose", false, "Maximal verbosity");


    Option RECURSIVITY_LIMIT = new Option("rl", "recursivity-limit", false,
            "Recursivity limit");


    Option AVOID_UNBOUND_TYPE_ATOMS = new Option("auta", "avoid-unbound-type-atoms", false,
            "Avoid unbound type atoms, e.g., type(x, y), i.e., bind always 'y' to a type");


    Option DO_NOT_EXPLOIT_MAX_LENGTH = new Option("deml", "do-not-exploit-max-length", false,
            "Do not exploit max length for speedup (requested by the reviewers of AMIE+). False by " +
                    "default.");


    Option DISABLE_QUERY_REWRITING = new Option("dqrw", "disable-query-rewriting", false,
            "Disable query rewriting and caching.");


    Option DISABLE_PERFECT_RULES = new Option("dpr", "disable-perfect-rules", false,
            "Disable perfect rules.");

    Option ONLY_OUTPUT = new Option("oout", "only-output", false,
            "If enabled, it activates only the output enhancements, that is, the confidence approximation " +
                    "and upper bounds. It overrides any other configuration that is incompatible.");

    Option FULL = new Option("full", "It enables all enhancements: " +
            "lossless heuristics and confidence approximation and upper bounds. It overrides any other configuration that is incompatible.");

    Option NO_HEURISTICS = new Option("noHeuristics", "Disable functionality heuristic, should be used with the -full option");

    Option NO_KB_REWRITE = new Option("noKbRewrite", "Prevent the KB to rewrite query when counting pairs");

    Option NO_KB_EXISTS_DETECTION = new Option("noKbExistsDetection", "Prevent the KB to detect existential variable on-the-fly " +
            "and to optimize the query");

    Option NO_SKYLINE = new Option("noSkyline", "Disable Skyline pruning of results");

    Option VARIABLE_ORDER = new Option("vo", "variableOrder", true,
            "Define the order of the variable in counting query among: app, fun (default), ifun");

    Option EXTRA_FILE = new Option("ef", "extraFile", true,
            "An additional text file whose interpretation depends " +
                    "on the selected mining assistant (bias)");

    Option OMMIT_STD_CONF = new Option("ostd", "ommit-std-conf", false,
            "Do not calculate standard confidence");

    Option ADAPTATIVE_INSTANTIATIONS = new Option("optimai", "adaptive-instantiations", false,
            "Prune instantiated rules that decrease too much the support of their parent rule (ratio 0.2)");

    Option MULTILINGUAL = new Option("mlg", "multilingual", false,
            "Parse labels language as new facts");

    Option DELIMITER = new Option("d", "delimiter", true,
            "Separator in input files (default: TAB)");

    Option DATALOG = new Option("datalog", "Enable datalog output.") ;

    interface Bias {
        String ONE_VAR = "oneVar" ;
        String DEFAULT = "default" ;
        String SIGNATURED = "signatured" ;
        String LAZY = "lazy" ;
        String LAZIT = "lazit"  ;

    }

    /**
     * Define the command line options supported by AMIE.
     *
     * @return
     */
    static Options DefineArgOptions() {
        // create the Options
        Options options = new Options();

        options.addOption(MIN_STD_CONFIDENCE);
        options.addOption(MIN_SUPPORT);
        options.addOption(MIN_INITIAL_SUPPORT);
        options.addOption(MIN_HEAD_COVERAGE);
        options.addOption(PRUNING_METRIC);
        options.addOption(OUTPUT_AT_END);
        options.addOption(BODY_EXCLUDED);
        options.addOption(HEAD_EXCLUDED);
        options.addOption(INSTANTIATION_EXCLUDED);
        options.addOption(MAX_DEPTH);
        options.addOption(MIN_PCA_CONFIDENCE);
        options.addOption(HEAD_TARGET_RELATIONS);
        options.addOption(BODY_TARGET_RELATIONS);
        options.addOption(INSTANTIATION_TARGET_RELATIONS);
        options.addOption(ALLOW_CONSTANTS);
        options.addOption(ONLY_CONSTANTS);
        options.addOption(COUNT_ALWAYS_ON_SUBJECT);
        options.addOption(BIAS);
        options.addOption(N_THREADS);
        options.addOption(OPTIM_CONFIDENCE_BOUNDS);
        options.addOption(VERBOSE);
        options.addOption(OPTIM_FUNC_HEURISTIC);
        options.addOption(RECURSIVITY_LIMIT);
        options.addOption(AVOID_UNBOUND_TYPE_ATOMS);
        options.addOption(DO_NOT_EXPLOIT_MAX_LENGTH);
        options.addOption(DISABLE_QUERY_REWRITING);
        options.addOption(DISABLE_PERFECT_RULES);
        options.addOption(ONLY_OUTPUT);
        options.addOption(FULL);
        options.addOption(NO_HEURISTICS);
        options.addOption(NO_KB_REWRITE);
        options.addOption(NO_KB_EXISTS_DETECTION);
        options.addOption(NO_SKYLINE);
        options.addOption(VARIABLE_ORDER);
        options.addOption(EXTRA_FILE);
        options.addOption(OUTPUT_FORMAT);
        options.addOption(OMMIT_STD_CONF);
        options.addOption(ADAPTATIVE_INSTANTIATIONS);
        options.addOption(MULTILINGUAL);
        options.addOption(DELIMITER);
        options.addOption(CACHE);
        options.addOption(CPOL);
        options.addOption(CSIZE);
        options.addOption(REMOTE_KB_MODE_CLIENT);
        options.addOption(REMOTE_KB_MODE_SERVER);
        options.addOption(SERVER_ADDRESS);
        options.addOption(CACHE);
        options.addOption(PORT) ;
        options.addOption(DATALOG) ;
        return options;
    }

    public String AMIE_CMD_LINE_SYNTAX = "AMIE [OPTIONS] <TSV FILES>" ;
    public String AMIE_PLUS_CMD_LINE_SYNTAX = "AMIE+ [OPTIONS] <.tsv INPUT FILES>" ;
    public String AMIE_PLUS = "AMIE+" ;

    static boolean isClientMode(CommandLine cli){
        return cli.hasOption(REMOTE_KB_MODE_CLIENT.getOpt()) || cli.hasOption(SERVER_ADDRESS.getOpt()) ;
    }

    static boolean isServerMode(CommandLine cli) {
        return cli.hasOption(REMOTE_KB_MODE_SERVER.getOpt()) || cli.hasOption(PORT.getOpt()) ;
    }

        // TODO Refactor options
    static boolean CheckForConflictingArguments(CommandLine cli, Options commandLineOptions) {

        HelpFormatter formatter = new HelpFormatter();

        if ( isClientMode(cli) && isServerMode(cli) ) {
            System.err.println("Remote KB client mode and remote KB server mode options are incompatible. Pick either one.");
            formatter.printHelp(AMIE_CMD_LINE_SYNTAX, commandLineOptions);
            return false;
        }

        if (cli.hasOption(CACHE.getOpt()) && !isClientMode(cli)) {
            System.err.println("Query cache can only be enabled with remote KB client mode (-remoteKBClientMode option)");
            formatter.printHelp(AMIE_CMD_LINE_SYNTAX, commandLineOptions);
            return false;
        }


        if (cli.hasOption(ONLY_OUTPUT.getOpt()) && cli.hasOption(FULL.getOpt())) {
            System.err.println("The options only-output and full are incompatible. Pick either one.");
            formatter.printHelp(AMIE_CMD_LINE_SYNTAX, commandLineOptions);
            return false;
        }

        if (cli.hasOption(HEAD_TARGET_RELATIONS.getOpt()) &&
                cli.hasOption(HEAD_EXCLUDED.getOpt())) {
            System.err.println("The options head-target-relations and head-excluded-relations cannot appear at the same time");
            System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
            return false;
        }

        if (cli.hasOption(BODY_TARGET_RELATIONS.getOpt()) &&
                cli.hasOption(BODY_EXCLUDED.getOpt())) {
            System.err.println("The options body-target-relations and body-excluded-relations cannot appear at the same time");
            formatter.printHelp(AMIE_PLUS, commandLineOptions);
            return false;
        }

        if (cli.hasOption(INSTANTIATION_TARGET_RELATIONS.getOpt()) &&
                cli.hasOption(INSTANTIATION_EXCLUDED.getOpt())) {
            System.err.println("The options instantiation-target-relations and instantiation-excluded-relations cannot appear at the same time");
            formatter.printHelp(AMIE_PLUS, commandLineOptions);
            return false;
        }

        if (cli.hasOption(INSTANTIATION_TARGET_RELATIONS.getOpt()) &&
                !(cli.hasOption(ALLOW_CONSTANTS.getOpt()) ||
                        cli.hasOption(ONLY_CONSTANTS.getOpt()))) {
            System.err.println("The option instantiation-target-relations should be used  with -const or -fconst");
            formatter.printHelp(AMIE_PLUS, commandLineOptions);
            return false;
        }

        if (cli.hasOption(INSTANTIATION_EXCLUDED.getOpt()) &&
                !(cli.hasOption(ALLOW_CONSTANTS.getOpt()) ||
                        cli.hasOption(ONLY_CONSTANTS.getOpt()))) {
            System.err.println("The option instantiation-excluded-relations should be used  with -const or -fconst");
            formatter.printHelp(AMIE_PLUS, commandLineOptions);
            return false;
        }

        if (cli.hasOption(MIN_SUPPORT.getOpt()) &&
                cli.hasOption(MIN_HEAD_COVERAGE.getOpt()) &&
                !cli.hasOption(PRUNING_METRIC.getOpt())) {
            System.err.println("Warning: Both -mins and -minhc are set but only the default pruning metric will be used");
        }

        return true;

    }
}
